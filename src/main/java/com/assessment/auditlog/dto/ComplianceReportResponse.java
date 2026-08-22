package com.assessment.auditlog.dto;

import java.util.List;

public record ComplianceReportResponse(
        String from,
        String to,
        long snapshotLastId,
        String snapshotLastRecordHash,
        List<ComplianceReportItemResponse> items,
        Long nextCursor,
        boolean hasMore) {

    public ComplianceReportResponse {
        items = List.copyOf(items);
    }
}
