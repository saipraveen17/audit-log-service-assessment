package com.assessment.auditlog.service;

import java.time.Instant;

public record ComplianceReportQuery(
        Instant from,
        Instant to,
        String actorId,
        String resourceId,
        String eventType,
        Long afterId,
        int limit,
        long snapshotLastId) {
}
