package com.assessment.auditlog.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RedactionRequest(
        @NotNull List<String> paths,
        @NotBlank String reason) {
}
