package com.assessment.auditlog.repository;

import java.util.List;

import com.assessment.auditlog.entity.AuditEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;

import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;

@Repository
public class AuditExportRepository {

    private final EntityManager entityManager;

    public AuditExportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<AuditEvent> findProofEventsThrough(long snapshotLastId) {
        TypedQuery<AuditEvent> query = entityManager.createQuery(
                """
                        select event
                        from AuditEvent event
                        where event.id <= :snapshotLastId
                        order by event.id asc
                        """,
                AuditEvent.class);
        query.setParameter("snapshotLastId", snapshotLastId);
        return readOnly(query).getResultList();
    }

    public List<SelectedEventRow> findSelectedRecords(String selectorType, String selectorValue, long snapshotLastId) {
        String selectorPredicate = switch (selectorType) {
            case "actorId" -> "event.actorId = :selectorValue";
            case "resourceId" -> "event.resourceId = :selectorValue";
            default -> throw new IllegalArgumentException("Unsupported export selector");
        };
        TypedQuery<SelectedEventRow> query = entityManager.createQuery(
                """
                        select new com.assessment.auditlog.repository.SelectedEventRow(
                            event,
                            case when marker.auditEventId is null then false else true end
                        )
                        from AuditEvent event
                        left join AuditArchiveMarker marker on marker.auditEventId = event.id
                        where event.id <= :snapshotLastId
                          and %s
                        order by event.id asc
                        """.formatted(selectorPredicate),
                SelectedEventRow.class);
        query.setParameter("snapshotLastId", snapshotLastId);
        query.setParameter("selectorValue", selectorValue);
        return readOnly(query).getResultList();
    }

    private <T> TypedQuery<T> readOnly(TypedQuery<T> query) {
        query.setFlushMode(FlushModeType.COMMIT);
        query.setHint(HibernateHints.HINT_READ_ONLY, true);
        return query;
    }
}
