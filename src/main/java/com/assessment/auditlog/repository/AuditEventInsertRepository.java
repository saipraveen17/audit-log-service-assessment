package com.assessment.auditlog.repository;

import com.assessment.auditlog.entity.AuditEvent;
import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

@Repository
public class AuditEventInsertRepository {

    private final EntityManager entityManager;

    public AuditEventInsertRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void insert(AuditEvent event) {
        entityManager.persist(event);
        entityManager.flush();
    }
}
