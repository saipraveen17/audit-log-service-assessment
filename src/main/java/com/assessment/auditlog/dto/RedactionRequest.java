package com.assessment.auditlog.dto;

import java.util.List;

import com.assessment.auditlog.service.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedactionRequest(
        @NotNull @Size(min = 1, max = InputLimits.MAX_SENSITIVE_PATHS)
        List<@Size(max = InputLimits.MAX_TEXT_LENGTH) String> paths,
        @NotBlank @Size(max = InputLimits.MAX_TEXT_LENGTH) String reason) {
}
