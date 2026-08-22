package com.assessment.auditlog.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuditEventRequest(
        @NotBlank String eventType,
        @NotBlank String actorId,
        @NotBlank String resourceType,
        @NotBlank String resourceId,
        @NotNull JsonNode payload,
        List<String> sensitivePaths) {
}
