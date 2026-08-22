package com.assessment.auditlog.dto;

public record AuditExportManifest(
        int bundleVersion,
        String selectorType,
        String selectorValue,
        String exportedAt,
        long snapshotLastId,
        String snapshotLastRecordHash,
        int selectedRecordCount,
        String hashAlgorithm,
        String signatureAlgorithm,
        String signingKeyId) {
}
