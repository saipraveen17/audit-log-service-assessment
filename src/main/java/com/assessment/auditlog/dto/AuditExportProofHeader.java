package com.assessment.auditlog.dto;

public record AuditExportProofHeader(
        long id,
        String contentHash,
        String previousHash,
        String recordHash,
        int hashVersion) {
}
