package com.assessment.auditlog.dto;

import java.util.List;

import com.assessment.auditlog.service.InputLimits;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAuditEventRequest(
        @NotBlank @Size(max = InputLimits.MAX_TEXT_LENGTH) String eventType,
        @NotBlank @Size(max = InputLimits.MAX_TEXT_LENGTH) String actorId,
        @NotBlank @Size(max = InputLimits.MAX_TEXT_LENGTH) String resourceType,
        @NotBlank @Size(max = InputLimits.MAX_TEXT_LENGTH) String resourceId,
        @NotNull JsonNode payload,
        @Size(max = InputLimits.MAX_SENSITIVE_PATHS)
        List<@Size(max = InputLimits.MAX_TEXT_LENGTH) String> sensitivePaths) {
}
