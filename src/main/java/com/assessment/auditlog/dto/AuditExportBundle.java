package com.assessment.auditlog.dto;

import java.util.List;

public record AuditExportBundle(
        AuditExportManifest manifest,
        List<AuditExportSelectedRecord> selectedRecords,
        List<AuditExportProofHeader> chainProofHeaders,
        String bundleDigest,
        String signature) {

    public AuditExportBundle {
        selectedRecords = List.copyOf(selectedRecords);
        chainProofHeaders = List.copyOf(chainProofHeaders);
    }
}
