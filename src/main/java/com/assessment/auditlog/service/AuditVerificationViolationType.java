package com.assessment.auditlog.service;

public enum AuditVerificationViolationType {
    ID_GAP,
    UNSUPPORTED_HASH_VERSION,
    CONTENT_HASH_MISMATCH,
    PREVIOUS_HASH_MISMATCH,
    RECORD_HASH_MISMATCH,
    CHAIN_HEAD_MISMATCH
}
