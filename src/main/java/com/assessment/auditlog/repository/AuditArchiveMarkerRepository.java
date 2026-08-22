package com.assessment.auditlog.repository;

import java.time.Instant;

import com.assessment.auditlog.entity.AuditArchiveMarker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.data.repository.Repository;

public interface AuditArchiveMarkerRepository extends Repository<AuditArchiveMarker, Long>, AuditArchiveMarkerRepositoryCustom {
}

interface AuditArchiveMarkerRepositoryCustom {

    int insertEligibleMarkers(Instant cutoff, Instant archivedAt, int retentionDays, String reason);

    long countEligibleMarkedEvents(Instant cutoff);
}

class AuditArchiveMarkerRepositoryImpl implements AuditArchiveMarkerRepositoryCustom {

    private final EntityManager entityManager;

    AuditArchiveMarkerRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int insertEligibleMarkers(Instant cutoff, Instant archivedAt, int retentionDays, String reason) {
        Query query = entityManager.createNativeQuery(
                """
                        insert into audit_archive_marker (audit_event_id, archived_at, retention_days, reason)
                        select e.id, :archivedAt, :retentionDays, :reason
                        from audit_event e
                        where e.timestamp < :cutoff
                        on conflict (audit_event_id) do nothing
                        """);
        query.setParameter("cutoff", cutoff);
        query.setParameter("archivedAt", archivedAt);
        query.setParameter("retentionDays", retentionDays);
        query.setParameter("reason", reason);
        return query.executeUpdate();
    }

    @Override
    public long countEligibleMarkedEvents(Instant cutoff) {
        Query query = entityManager.createNativeQuery(
                """
                        select count(*)
                        from audit_event e
                        join audit_archive_marker marker on marker.audit_event_id = e.id
                        where e.timestamp < :cutoff
                        """);
        query.setParameter("cutoff", cutoff);
        return ((Number) query.getSingleResult()).longValue();
    }
}
