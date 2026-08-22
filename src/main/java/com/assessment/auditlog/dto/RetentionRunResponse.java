package com.assessment.auditlog.dto;

public record RetentionRunResponse(
        int retentionDays,
        long archivedCount,
        long alreadyArchivedCount) {
}
