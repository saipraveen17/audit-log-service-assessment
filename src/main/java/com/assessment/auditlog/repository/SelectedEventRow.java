package com.assessment.auditlog.repository;

import com.assessment.auditlog.entity.AuditEvent;

public record SelectedEventRow(AuditEvent event, boolean archived) {
}
