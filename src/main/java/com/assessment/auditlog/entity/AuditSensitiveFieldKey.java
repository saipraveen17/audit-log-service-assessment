package com.assessment.auditlog.entity;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "audit_sensitive_field_key", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_audit_sensitive_field_key_event_path",
                columnNames = {"audit_event_id", "json_pointer"})
})
public class AuditSensitiveFieldKey {

    @Id
    @Column(name = "key_id", nullable = false, updatable = false)
    private UUID keyId;

    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private Long auditEventId;

    @Column(name = "json_pointer", nullable = false, updatable = false, length = 255)
    private String jsonPointer;

    @Column(name = "wrapped_key")
    private byte[] wrappedKey;

    @Column(name = "wrapping_iv")
    private byte[] wrappingIv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "redacted_at")
    private Instant redactedAt;

    @Column(name = "redaction_reason", length = 255)
    private String redactionReason;

    @Column(name = "redacted_by", length = 255)
    private String redactedBy;

    protected AuditSensitiveFieldKey() {
    }

    public AuditSensitiveFieldKey(
            UUID keyId,
            Long auditEventId,
            String jsonPointer,
            byte[] wrappedKey,
            byte[] wrappingIv,
            Instant createdAt) {
        this.keyId = keyId;
        this.auditEventId = auditEventId;
        this.jsonPointer = jsonPointer;
        this.wrappedKey = copy(wrappedKey);
        this.wrappingIv = copy(wrappingIv);
        this.createdAt = createdAt;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public Long getAuditEventId() {
        return auditEventId;
    }

    public String getJsonPointer() {
        return jsonPointer;
    }

    public byte[] getWrappedKey() {
        return copy(wrappedKey);
    }

    public byte[] getWrappingIv() {
        return copy(wrappingIv);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRedactedAt() {
        return redactedAt;
    }

    public String getRedactionReason() {
        return redactionReason;
    }

    public String getRedactedBy() {
        return redactedBy;
    }

    public boolean isRedacted() {
        return wrappedKey == null
                && wrappingIv == null
                && redactedAt != null
                && redactionReason != null
                && !redactionReason.isBlank()
                && redactedBy != null
                && !redactedBy.isBlank();
    }

    public void redact(Instant redactedAt, String redactionReason, String redactedBy) {
        if (isRedacted()) {
            return;
        }
        if (wrappedKey == null || wrappingIv == null) {
            throw new IllegalStateException("Sensitive field key state is incomplete");
        }
        Arrays.fill(wrappedKey, (byte) 0);
        Arrays.fill(wrappingIv, (byte) 0);
        this.wrappedKey = null;
        this.wrappingIv = null;
        this.redactedAt = redactedAt;
        this.redactionReason = redactionReason;
        this.redactedBy = redactedBy;
    }

    private static byte[] copy(byte[] value) {
        return value == null ? null : value.clone();
    }
}
