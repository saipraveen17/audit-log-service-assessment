package com.assessment.auditlog.dto;

import com.assessment.auditlog.service.AuditVerificationViolationType;

public record AuditVerificationResponse(
        boolean intact,
        long verifiedRecordCount,
        long snapshotLastId,
        String snapshotLastRecordHash,
        Long firstInconsistentId,
        AuditVerificationViolationType violationType) {
}
