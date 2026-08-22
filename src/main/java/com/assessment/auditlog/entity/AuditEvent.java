package com.assessment.auditlog.entity;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_event", indexes = {
        @Index(name = "idx_audit_event_actor_id", columnList = "actor_id, id"),
        @Index(name = "idx_audit_event_resource", columnList = "resource_type, resource_id, id"),
        @Index(name = "idx_audit_event_event_type", columnList = "event_type, id"),
        @Index(name = "idx_audit_event_timestamp", columnList = "timestamp, id")
})
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private String actorId;

    @Column(name = "resource_type", nullable = false, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private JsonNode committedPayload;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "content_hash", nullable = false, updatable = false, length = 64)
    private String contentHash;

    @Column(name = "previous_hash", nullable = false, updatable = false, length = 64)
    private String previousHash;

    @Column(name = "record_hash", nullable = false, updatable = false, length = 64)
    private String recordHash;

    @Column(name = "hash_version", nullable = false, updatable = false)
    private int hashVersion;

    protected AuditEvent() {
    }

    public AuditEvent(
            Long id,
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode committedPayload,
            Instant timestamp,
            String contentHash,
            String previousHash,
            String recordHash,
            int hashVersion) {
        this.id = id;
        this.eventType = eventType;
        this.actorId = actorId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.committedPayload = committedPayload;
        this.timestamp = timestamp;
        this.contentHash = contentHash;
        this.previousHash = previousHash;
        this.recordHash = recordHash;
        this.hashVersion = hashVersion;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public JsonNode getCommittedPayload() {
        return committedPayload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getRecordHash() {
        return recordHash;
    }

    public int getHashVersion() {
        return hashVersion;
    }
}
