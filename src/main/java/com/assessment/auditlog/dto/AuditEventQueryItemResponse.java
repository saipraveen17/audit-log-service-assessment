package com.assessment.auditlog.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditEventQueryItemResponse(
        long id,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        String timestamp,
        String recordHash,
        boolean archived) {

    public AuditEventQueryItemResponse {
        payload = payload == null ? null : payload.deepCopy();
    }

    @Override
    public JsonNode payload() {
        return payload == null ? null : payload.deepCopy();
    }
}
