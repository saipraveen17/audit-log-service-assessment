package com.assessment.auditlog.service;

import java.util.List;

import com.assessment.auditlog.dto.AuditVerificationResponse;
import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditChainStateRepository;
import com.assessment.auditlog.repository.AuditEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditVerificationService {

    private final AuditChainStateRepository chainStateRepository;

    private final AuditEventRepository auditEventRepository;

    private final AuditHashService auditHashService;

    public AuditVerificationService(
            AuditChainStateRepository chainStateRepository,
            AuditEventRepository auditEventRepository,
            AuditHashService auditHashService) {
        this.chainStateRepository = chainStateRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditHashService = auditHashService;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AuditVerificationResponse verify() {
        AuditChainState snapshot = chainStateRepository.findById(AuditChainState.GLOBAL_NAME)
                .orElseThrow(() -> new IllegalStateException("Audit chain state is not initialized"));
        long snapshotLastId = snapshot.getLastId();
        String snapshotLastRecordHash = snapshot.getLastRecordHash();

        List<AuditEvent> events = auditEventRepository.findAllByOrderByIdAsc();
        return verifySnapshot(snapshotLastId, snapshotLastRecordHash, events);
    }

    public AuditVerificationResponse verifySnapshot(
            long snapshotLastId,
            String snapshotLastRecordHash,
            List<AuditEvent> events) {
        long expectedId = 1;
        long verifiedRecordCount = 0;
        String expectedPreviousHash = AuditHashService.GENESIS_HASH;
        long lastVerifiedId = 0;

        for (AuditEvent event : events) {
            long currentId = event.getId();
            if (currentId > snapshotLastId) {
                if (expectedId <= snapshotLastId) {
                    return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, expectedId,
                            AuditVerificationViolationType.ID_GAP);
                }
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, currentId,
                        AuditVerificationViolationType.CHAIN_HEAD_MISMATCH);
            }
            if (currentId != expectedId) {
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, expectedId,
                        AuditVerificationViolationType.ID_GAP);
            }
            if (event.getHashVersion() != AuditHashService.HASH_VERSION) {
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, currentId,
                        AuditVerificationViolationType.UNSUPPORTED_HASH_VERSION);
            }

            String recalculatedContentHash = auditHashService.contentHash(
                    event.getEventType(),
                    event.getActorId(),
                    event.getResourceType(),
                    event.getResourceId(),
                    event.getCommittedPayload(),
                    event.getTimestamp());
            if (!recalculatedContentHash.equals(event.getContentHash())) {
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, currentId,
                        AuditVerificationViolationType.CONTENT_HASH_MISMATCH);
            }
            if (!expectedPreviousHash.equals(event.getPreviousHash())) {
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, currentId,
                        AuditVerificationViolationType.PREVIOUS_HASH_MISMATCH);
            }

            String recalculatedRecordHash = auditHashService.recordHash(
                    event.getHashVersion(),
                    currentId,
                    recalculatedContentHash,
                    event.getPreviousHash());
            if (!recalculatedRecordHash.equals(event.getRecordHash())) {
                return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, currentId,
                        AuditVerificationViolationType.RECORD_HASH_MISMATCH);
            }

            verifiedRecordCount++;
            lastVerifiedId = currentId;
            expectedId++;
            expectedPreviousHash = recalculatedRecordHash;
        }

        if (expectedId <= snapshotLastId) {
            return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, expectedId,
                    AuditVerificationViolationType.ID_GAP);
        }
        if (lastVerifiedId != snapshotLastId || !expectedPreviousHash.equals(snapshotLastRecordHash)) {
            Long inconsistentId = snapshotLastId == 0 ? null : snapshotLastId;
            return broken(verifiedRecordCount, snapshotLastId, snapshotLastRecordHash, inconsistentId,
                    AuditVerificationViolationType.CHAIN_HEAD_MISMATCH);
        }

        return new AuditVerificationResponse(
                true,
                verifiedRecordCount,
                snapshotLastId,
                snapshotLastRecordHash,
                null,
                null);
    }

    private AuditVerificationResponse broken(
            long verifiedRecordCount,
            long snapshotLastId,
            String snapshotLastRecordHash,
            Long firstInconsistentId,
            AuditVerificationViolationType violationType) {
        return new AuditVerificationResponse(
                false,
                verifiedRecordCount,
                snapshotLastId,
                snapshotLastRecordHash,
                firstInconsistentId,
                violationType);
    }
}
