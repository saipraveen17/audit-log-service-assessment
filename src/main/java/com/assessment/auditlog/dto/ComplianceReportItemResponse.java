package com.assessment.auditlog.dto;

public record ComplianceReportItemResponse(
        long id,
        String actorId,
        String resourceId,
        String eventType,
        String timestamp,
        String contentHash,
        String recordHash,
        boolean archived) {
}
