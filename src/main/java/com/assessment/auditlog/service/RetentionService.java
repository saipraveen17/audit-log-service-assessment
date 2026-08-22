package com.assessment.auditlog.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.assessment.auditlog.config.RetentionProperties;
import com.assessment.auditlog.dto.RetentionRunResponse;
import com.assessment.auditlog.repository.AuditArchiveMarkerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetentionService {

    public static final String RETENTION_REASON = "RETENTION_POLICY";

    private final Clock clock;

    private final RetentionProperties retentionProperties;

    private final AuditArchiveMarkerRepository archiveMarkerRepository;

    public RetentionService(
            Clock clock,
            RetentionProperties retentionProperties,
            AuditArchiveMarkerRepository archiveMarkerRepository) {
        this.clock = clock;
        this.retentionProperties = retentionProperties;
        this.archiveMarkerRepository = archiveMarkerRepository;
    }

    @Transactional
    public RetentionRunResponse runRetention() {
        int retentionDays = retentionProperties.getRetentionDays();
        Instant archivedAt = TimeFormats.truncateToMillis(clock.instant());
        Instant cutoff = archivedAt.minus(retentionDays, ChronoUnit.DAYS);
        int archivedCount = archiveMarkerRepository.insertEligibleMarkers(
                cutoff,
                archivedAt,
                retentionDays,
                RETENTION_REASON);
        long alreadyArchivedCount = archiveMarkerRepository.countEligibleMarkedEvents(cutoff) - archivedCount;
        return new RetentionRunResponse(retentionDays, archivedCount, alreadyArchivedCount);
    }
}
