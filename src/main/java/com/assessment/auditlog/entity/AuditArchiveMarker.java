package com.assessment.auditlog.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_archive_marker")
public class AuditArchiveMarker {

    @Id
    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private Long auditEventId;

    @Column(name = "archived_at", nullable = false, updatable = false)
    private Instant archivedAt;

    @Column(name = "retention_days", nullable = false, updatable = false)
    private int retentionDays;

    @Column(name = "reason", nullable = false, updatable = false)
    private String reason;

    protected AuditArchiveMarker() {
    }

    public AuditArchiveMarker(Long auditEventId, Instant archivedAt, int retentionDays, String reason) {
        this.auditEventId = auditEventId;
        this.archivedAt = archivedAt;
        this.retentionDays = retentionDays;
        this.reason = reason;
    }

    public Long getAuditEventId() {
        return auditEventId;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getReason() {
        return reason;
    }
}
