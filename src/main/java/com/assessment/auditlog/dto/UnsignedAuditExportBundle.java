package com.assessment.auditlog.dto;

import java.util.List;

public record UnsignedAuditExportBundle(
        AuditExportManifest manifest,
        List<AuditExportSelectedRecord> selectedRecords,
        List<AuditExportProofHeader> chainProofHeaders) {

    public UnsignedAuditExportBundle {
        selectedRecords = List.copyOf(selectedRecords);
        chainProofHeaders = List.copyOf(chainProofHeaders);
    }
}
