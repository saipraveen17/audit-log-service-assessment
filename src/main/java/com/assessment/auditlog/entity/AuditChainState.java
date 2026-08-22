package com.assessment.auditlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainState {

    public static final String GLOBAL_NAME = "GLOBAL";

    @Id
    @Column(name = "name", nullable = false, updatable = false)
    private String name;

    @Column(name = "last_id", nullable = false)
    private long lastId;

    @Column(name = "last_record_hash", nullable = false, length = 64)
    private String lastRecordHash;

    protected AuditChainState() {
    }

    public AuditChainState(String name, long lastId, String lastRecordHash) {
        this.name = name;
        this.lastId = lastId;
        this.lastRecordHash = lastRecordHash;
    }

    public String getName() {
        return name;
    }

    public long getLastId() {
        return lastId;
    }

    public String getLastRecordHash() {
        return lastRecordHash;
    }

    public void advanceTo(long newLastId, String newLastRecordHash) {
        this.lastId = newLastId;
        this.lastRecordHash = newLastRecordHash;
    }
}
