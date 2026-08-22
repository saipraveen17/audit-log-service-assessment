package com.assessment.auditlog.dto;

import java.util.List;

public record AuditEventQueryResponse(
        List<AuditEventQueryItemResponse> items,
        Long nextCursor,
        boolean hasMore) {

    public AuditEventQueryResponse {
        items = List.copyOf(items);
    }
}
