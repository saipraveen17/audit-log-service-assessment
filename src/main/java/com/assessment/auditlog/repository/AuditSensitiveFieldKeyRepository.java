package com.assessment.auditlog.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.assessment.auditlog.entity.AuditSensitiveFieldKey;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

public interface AuditSensitiveFieldKeyRepository extends Repository<AuditSensitiveFieldKey, UUID> {

    List<AuditSensitiveFieldKey> saveAll(Iterable<AuditSensitiveFieldKey> keys);

    List<AuditSensitiveFieldKey> findByAuditEventId(Long auditEventId);

    List<AuditSensitiveFieldKey> findByAuditEventIdIn(Collection<Long> auditEventIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuditSensitiveFieldKey> findByAuditEventIdAndJsonPointerIn(Long auditEventId, Collection<String> jsonPointers);
}
