package com.assessment.auditlog.repository;

import java.util.Optional;

import com.assessment.auditlog.entity.AuditChainState;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditChainStateRepository extends JpaRepository<AuditChainState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from AuditChainState state where state.name = :name")
    Optional<AuditChainState> findByNameForUpdate(@Param("name") String name);
}
