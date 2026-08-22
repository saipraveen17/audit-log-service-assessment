package com.assessment.auditlog.repository;

import java.util.List;
import java.util.Optional;

import com.assessment.auditlog.entity.AuditEvent;

import org.springframework.data.repository.Repository;

public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    Optional<AuditEvent> findById(Long id);

    List<AuditEvent> findAllByOrderByIdAsc();

    long count();
}
