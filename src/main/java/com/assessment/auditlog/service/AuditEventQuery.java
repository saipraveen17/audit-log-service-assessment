package com.assessment.auditlog.service;

import java.time.Instant;

public record AuditEventQuery(
        String actorId,
        String resourceType,
        String resourceId,
        String eventType,
        Instant from,
        Instant to,
        Long afterId,
        int limit) {
}
