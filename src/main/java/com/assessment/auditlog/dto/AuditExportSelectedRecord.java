package com.assessment.auditlog.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditExportSelectedRecord(
        long id,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode committedPayload,
        String timestamp,
        String contentHash,
        String previousHash,
        String recordHash,
        int hashVersion,
        boolean archived) {

    public AuditExportSelectedRecord {
        committedPayload = committedPayload.deepCopy();
    }

    @Override
    public JsonNode committedPayload() {
        return committedPayload.deepCopy();
    }
}
