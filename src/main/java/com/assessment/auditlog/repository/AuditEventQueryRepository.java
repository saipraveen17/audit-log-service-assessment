package com.assessment.auditlog.repository;

import java.util.ArrayList;
import java.util.List;

import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.service.AuditEventQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;

@Repository
public class AuditEventQueryRepository {

    private final EntityManager entityManager;

    public AuditEventQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<AuditEvent> find(AuditEventQuery query, int fetchLimit) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEvent> criteriaQuery = criteriaBuilder.createQuery(AuditEvent.class);
        Root<AuditEvent> event = criteriaQuery.from(AuditEvent.class);
        List<Predicate> predicates = new ArrayList<>();

        if (query.afterId() != null) {
            predicates.add(criteriaBuilder.greaterThan(event.get("id"), query.afterId()));
        }
        if (query.actorId() != null) {
            predicates.add(criteriaBuilder.equal(event.get("actorId"), query.actorId()));
        }
        if (query.resourceType() != null) {
            predicates.add(criteriaBuilder.equal(event.get("resourceType"), query.resourceType()));
        }
        if (query.resourceId() != null) {
            predicates.add(criteriaBuilder.equal(event.get("resourceId"), query.resourceId()));
        }
        if (query.eventType() != null) {
            predicates.add(criteriaBuilder.equal(event.get("eventType"), query.eventType()));
        }
        if (query.from() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(event.get("timestamp"), query.from()));
        }
        if (query.to() != null) {
            predicates.add(criteriaBuilder.lessThan(event.get("timestamp"), query.to()));
        }

        criteriaQuery.select(event)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.asc(event.get("id")));

        TypedQuery<AuditEvent> typedQuery = entityManager.createQuery(criteriaQuery)
                .setMaxResults(fetchLimit)
                .setFlushMode(FlushModeType.COMMIT)
                .setHint(HibernateHints.HINT_READ_ONLY, true);
        return typedQuery.getResultList();
    }
}
