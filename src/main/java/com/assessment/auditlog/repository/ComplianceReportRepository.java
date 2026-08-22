package com.assessment.auditlog.repository;

import java.util.List;

import com.assessment.auditlog.service.ComplianceReportQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;

import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;

@Repository
public class ComplianceReportRepository {

    private static final String CLIENT_ACCOUNT = "CLIENT_ACCOUNT";

    private final EntityManager entityManager;

    public ComplianceReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<SelectedEventRow> find(ComplianceReportQuery query, int fetchLimit) {
        StringBuilder jpql = new StringBuilder("""
                select new com.assessment.auditlog.repository.SelectedEventRow(
                    event,
                    case when marker.auditEventId is null then false else true end
                )
                from AuditEvent event
                left join AuditArchiveMarker marker on marker.auditEventId = event.id
                where event.resourceType = :resourceType
                  and event.id <= :snapshotLastId
                  and event.timestamp >= :from
                  and event.timestamp < :to
                """);
        if (query.afterId() != null) {
            jpql.append(" and event.id > :afterId");
        }
        if (query.actorId() != null) {
            jpql.append(" and event.actorId = :actorId");
        }
        if (query.resourceId() != null) {
            jpql.append(" and event.resourceId = :resourceId");
        }
        if (query.eventType() != null) {
            jpql.append(" and event.eventType = :eventType");
        }
        jpql.append(" order by event.id asc");

        TypedQuery<SelectedEventRow> typedQuery = entityManager.createQuery(jpql.toString(), SelectedEventRow.class)
                .setMaxResults(fetchLimit)
                .setFlushMode(FlushModeType.COMMIT)
                .setHint(HibernateHints.HINT_READ_ONLY, true);
        typedQuery.setParameter("resourceType", CLIENT_ACCOUNT);
        typedQuery.setParameter("snapshotLastId", query.snapshotLastId());
        typedQuery.setParameter("from", query.from());
        typedQuery.setParameter("to", query.to());
        if (query.afterId() != null) {
            typedQuery.setParameter("afterId", query.afterId());
        }
        if (query.actorId() != null) {
            typedQuery.setParameter("actorId", query.actorId());
        }
        if (query.resourceId() != null) {
            typedQuery.setParameter("resourceId", query.resourceId());
        }
        if (query.eventType() != null) {
            typedQuery.setParameter("eventType", query.eventType());
        }
        return typedQuery.getResultList();
    }
}
