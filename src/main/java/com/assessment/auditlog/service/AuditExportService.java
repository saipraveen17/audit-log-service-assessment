package com.assessment.auditlog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.assessment.auditlog.config.ExportProperties;
import com.assessment.auditlog.dto.AuditExportBundle;
import com.assessment.auditlog.dto.AuditExportManifest;
import com.assessment.auditlog.dto.AuditExportProofHeader;
import com.assessment.auditlog.dto.AuditExportSelectedRecord;
import com.assessment.auditlog.dto.AuditVerificationResponse;
import com.assessment.auditlog.dto.UnsignedAuditExportBundle;
import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditChainStateRepository;
import com.assessment.auditlog.repository.AuditExportRepository;
import com.assessment.auditlog.repository.SelectedEventRow;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditExportService {

    private static final int BUNDLE_VERSION = 1;

    private final Clock clock;

    private final ExportProperties exportProperties;

    private final AuditChainStateRepository chainStateRepository;

    private final AuditExportRepository auditExportRepository;

    private final AuditVerificationService auditVerificationService;

    private final AuditExportCryptoService cryptoService;

    public AuditExportService(
            Clock clock,
            ExportProperties exportProperties,
            AuditChainStateRepository chainStateRepository,
            AuditExportRepository auditExportRepository,
            AuditVerificationService auditVerificationService,
            AuditExportCryptoService cryptoService) {
        this.clock = clock;
        this.exportProperties = exportProperties;
        this.chainStateRepository = chainStateRepository;
        this.auditExportRepository = auditExportRepository;
        this.auditVerificationService = auditVerificationService;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AuditExportBundle export(String actorId, String resourceId) {
        Selector selector = validateSelector(actorId, resourceId);
        AuditChainState snapshot = chainStateRepository.findById(AuditChainState.GLOBAL_NAME)
                .orElseThrow(() -> new IllegalStateException("Audit chain state is not initialized"));
        long snapshotLastId = snapshot.getLastId();
        String snapshotLastRecordHash = snapshot.getLastRecordHash();

        List<AuditEvent> proofEvents = auditExportRepository.findProofEventsThrough(snapshotLastId);
        AuditVerificationResponse verification = auditVerificationService.verifySnapshot(
                snapshotLastId,
                snapshotLastRecordHash,
                proofEvents);
        if (!verification.intact()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit chain is not intact");
        }

        List<SelectedEventRow> selectedRows = auditExportRepository.findSelectedRecords(
                selector.type(),
                selector.value(),
                snapshotLastId);
        List<AuditExportSelectedRecord> selectedRecords = selectedRows.stream()
                .map(this::toSelectedRecord)
                .toList();
        List<AuditExportProofHeader> proofHeaders = proofEvents.stream()
                .map(this::toProofHeader)
                .toList();
        Instant exportedAt = TimeFormats.truncateToMillis(clock.instant());
        AuditExportManifest manifest = new AuditExportManifest(
                BUNDLE_VERSION,
                selector.type(),
                selector.value(),
                TimeFormats.formatUtcMillis(exportedAt),
                snapshotLastId,
                snapshotLastRecordHash,
                selectedRecords.size(),
                "SHA-256",
                "Ed25519",
                exportProperties.getSigningKeyId());
        UnsignedAuditExportBundle unsignedBundle = new UnsignedAuditExportBundle(
                manifest,
                selectedRecords,
                proofHeaders);
        AuditExportCryptoService.SignedDigest signedDigest = cryptoService.sign(
                unsignedBundle,
                exportProperties.privateKey());
        return new AuditExportBundle(
                manifest,
                selectedRecords,
                proofHeaders,
                signedDigest.digestHex(),
                signedDigest.signatureBase64());
    }

    private Selector validateSelector(String actorId, String resourceId) {
        boolean actorPresent = actorId != null;
        boolean resourcePresent = resourceId != null;
        if (actorPresent == resourcePresent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one export selector is required");
        }
        if (actorPresent) {
            return new Selector("actorId", requireNonBlank(actorId));
        }
        return new Selector("resourceId", requireNonBlank(resourceId));
    }

    private String requireNonBlank(String value) {
        return InputLimits.requireNonBlank(value, "Export selector");
    }

    private AuditExportSelectedRecord toSelectedRecord(SelectedEventRow row) {
        AuditEvent event = row.event();
        return new AuditExportSelectedRecord(
                event.getId(),
                event.getEventType(),
                event.getActorId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCommittedPayload(),
                TimeFormats.formatUtcMillis(event.getTimestamp()),
                event.getContentHash(),
                event.getPreviousHash(),
                event.getRecordHash(),
                event.getHashVersion(),
                row.archived());
    }

    private AuditExportProofHeader toProofHeader(AuditEvent event) {
        return new AuditExportProofHeader(
                event.getId(),
                event.getContentHash(),
                event.getPreviousHash(),
                event.getRecordHash(),
                event.getHashVersion());
    }

    private record Selector(String type, String value) {
    }
}
