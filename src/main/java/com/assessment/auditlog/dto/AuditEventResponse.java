package com.assessment.auditlog.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditEventResponse(
        long id,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        String timestamp,
        String contentHash,
        String previousHash,
        String recordHash,
        int hashVersion,
        boolean archived) {

    public AuditEventResponse {
        payload = payload == null ? null : payload.deepCopy();
    }

    @Override
    public JsonNode payload() {
        return payload == null ? null : payload.deepCopy();
    }
}
