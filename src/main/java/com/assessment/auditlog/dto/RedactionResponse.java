package com.assessment.auditlog.dto;

import java.util.List;

public record RedactionResponse(
        long id,
        List<String> redactedPaths,
        List<String> alreadyRedactedPaths,
        boolean payloadChanged) {

    public RedactionResponse {
        redactedPaths = List.copyOf(redactedPaths);
        alreadyRedactedPaths = List.copyOf(alreadyRedactedPaths);
    }
}
