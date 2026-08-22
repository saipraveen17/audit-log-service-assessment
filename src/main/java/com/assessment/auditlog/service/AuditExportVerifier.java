package com.assessment.auditlog.service;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import com.assessment.auditlog.dto.AuditExportBundle;
import com.assessment.auditlog.dto.AuditExportProofHeader;
import com.assessment.auditlog.dto.AuditExportSelectedRecord;

import org.springframework.stereotype.Service;

@Service
public class AuditExportVerifier {

    private static final int BUNDLE_VERSION = 1;

    private final AuditExportCryptoService cryptoService;

    private final AuditHashService auditHashService;

    public AuditExportVerifier(AuditExportCryptoService cryptoService, AuditHashService auditHashService) {
        this.cryptoService = cryptoService;
        this.auditHashService = auditHashService;
    }

    public boolean verify(AuditExportBundle bundle, PublicKey trustedPublicKey) {
        if (bundle == null || trustedPublicKey == null || bundle.manifest() == null
                || bundle.selectedRecords() == null || bundle.chainProofHeaders() == null
                || bundle.bundleDigest() == null || bundle.signature() == null) {
            return false;
        }
        if (!cryptoService.verifySignature(bundle, trustedPublicKey)) {
            return false;
        }
        return verifyStructure(bundle);
    }

    private boolean verifyStructure(AuditExportBundle bundle) {
        if (bundle.manifest().bundleVersion() != BUNDLE_VERSION
                || !"SHA-256".equals(bundle.manifest().hashAlgorithm())
                || !"Ed25519".equals(bundle.manifest().signatureAlgorithm())
                || (!"actorId".equals(bundle.manifest().selectorType())
                && !"resourceId".equals(bundle.manifest().selectorType()))) {
            return false;
        }
        Map<Long, AuditExportProofHeader> proofHeadersById = new HashMap<>();
        long expectedId = 1;
        String expectedPreviousHash = AuditHashService.GENESIS_HASH;
        AuditExportProofHeader lastHeader = null;
        for (AuditExportProofHeader header : bundle.chainProofHeaders()) {
            if (header.id() != expectedId || header.hashVersion() != AuditHashService.HASH_VERSION) {
                return false;
            }
            if (!expectedPreviousHash.equals(header.previousHash())) {
                return false;
            }
            String recalculatedRecordHash = auditHashService.recordHash(
                    header.hashVersion(),
                    header.id(),
                    header.contentHash(),
                    header.previousHash());
            if (!recalculatedRecordHash.equals(header.recordHash())) {
                return false;
            }
            proofHeadersById.put(header.id(), header);
            lastHeader = header;
            expectedPreviousHash = recalculatedRecordHash;
            expectedId++;
        }

        if (!matchesSnapshotHead(bundle, lastHeader)) {
            return false;
        }
        if (bundle.manifest().selectedRecordCount() != bundle.selectedRecords().size()) {
            return false;
        }
        for (AuditExportSelectedRecord record : bundle.selectedRecords()) {
            AuditExportProofHeader header = proofHeadersById.get(record.id());
            if (header == null || !matchesSelector(bundle, record)) {
                return false;
            }
            String recalculatedContentHash;
            try {
                recalculatedContentHash = auditHashService.contentHash(
                        record.eventType(),
                        record.actorId(),
                        record.resourceType(),
                        record.resourceId(),
                        record.committedPayload(),
                        TimeFormats.parseInstant(record.timestamp()));
            } catch (IllegalArgumentException exception) {
                return false;
            }
            if (!recalculatedContentHash.equals(record.contentHash())
                    || !record.contentHash().equals(header.contentHash())
                    || !record.previousHash().equals(header.previousHash())
                    || !record.recordHash().equals(header.recordHash())
                    || record.hashVersion() != header.hashVersion()) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesSnapshotHead(AuditExportBundle bundle, AuditExportProofHeader lastHeader) {
        if (bundle.manifest().snapshotLastId() == 0) {
            return bundle.chainProofHeaders().isEmpty()
                    && AuditHashService.GENESIS_HASH.equals(bundle.manifest().snapshotLastRecordHash());
        }
        return lastHeader != null
                && lastHeader.id() == bundle.manifest().snapshotLastId()
                && lastHeader.recordHash().equals(bundle.manifest().snapshotLastRecordHash());
    }

    private boolean matchesSelector(AuditExportBundle bundle, AuditExportSelectedRecord record) {
        if ("actorId".equals(bundle.manifest().selectorType())) {
            return bundle.manifest().selectorValue().equals(record.actorId());
        }
        if ("resourceId".equals(bundle.manifest().selectorType())) {
            return bundle.manifest().selectorValue().equals(record.resourceId());
        }
        return false;
    }
}
