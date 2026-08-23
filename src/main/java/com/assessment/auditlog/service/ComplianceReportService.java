package com.assessment.auditlog.service;

import java.time.Instant;
import java.util.List;

import com.assessment.auditlog.dto.AuditVerificationResponse;
import com.assessment.auditlog.dto.ComplianceReportItemResponse;
import com.assessment.auditlog.dto.ComplianceReportResponse;
import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditChainStateRepository;
import com.assessment.auditlog.repository.AuditExportRepository;
import com.assessment.auditlog.repository.ComplianceReportRepository;
import com.assessment.auditlog.repository.SelectedEventRow;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComplianceReportService {

    private static final int DEFAULT_LIMIT = 50;

    private static final int MAX_LIMIT = 200;

    private final AuditChainStateRepository chainStateRepository;

    private final AuditExportRepository auditExportRepository;

    private final AuditVerificationService auditVerificationService;

    private final ComplianceReportRepository complianceReportRepository;

    public ComplianceReportService(
            AuditChainStateRepository chainStateRepository,
            AuditExportRepository auditExportRepository,
            AuditVerificationService auditVerificationService,
            ComplianceReportRepository complianceReportRepository) {
        this.chainStateRepository = chainStateRepository;
        this.auditExportRepository = auditExportRepository;
        this.auditVerificationService = auditVerificationService;
        this.complianceReportRepository = complianceReportRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ComplianceReportResponse report(
            String from,
            String to,
            String actorId,
            String resourceId,
            String eventType,
            String afterId,
            String limit) {
        Instant parsedFrom = parseRequiredTimestamp(from, "from");
        Instant parsedTo = parseRequiredTimestamp(to, "to");
        validateTimeRange(parsedFrom, parsedTo);

        AuditChainState snapshot = chainStateRepository.findById(AuditChainState.GLOBAL_NAME)
                .orElseThrow(() -> new IllegalStateException("Audit chain state is not initialized"));
        long snapshotLastId = snapshot.getLastId();
        String snapshotLastRecordHash = snapshot.getLastRecordHash();
        List<AuditEvent> proofEvents = auditExportRepository.findProofEventsThrough(snapshotLastId);
        AuditVerificationResponse verification = auditVerificationService.verifySnapshot(
                snapshotLastId,
                snapshotLastRecordHash,
                proofEvents);
        if (!verification.intact()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit chain is not intact");
        }

        ComplianceReportQuery query = new ComplianceReportQuery(
                parsedFrom,
                parsedTo,
                InputLimits.requireNonBlankIfPresent(actorId, "actorId"),
                InputLimits.requireNonBlankIfPresent(resourceId, "resourceId"),
                InputLimits.requireNonBlankIfPresent(eventType, "eventType"),
                parseAfterId(afterId),
                parseLimit(limit),
                snapshotLastId);
        List<SelectedEventRow> fetchedRows = complianceReportRepository.find(query, query.limit() + 1);
        boolean hasMore = fetchedRows.size() > query.limit();
        List<SelectedEventRow> visibleRows = fetchedRows.stream()
                .limit(query.limit())
                .toList();
        List<ComplianceReportItemResponse> items = visibleRows.stream()
                .map(this::toItem)
                .toList();
        Long nextCursor = items.isEmpty() ? null : items.getLast().id();
        return new ComplianceReportResponse(
                TimeFormats.formatUtcMillis(parsedFrom),
                TimeFormats.formatUtcMillis(parsedTo),
                snapshotLastId,
                snapshotLastRecordHash,
                items,
                nextCursor,
                hasMore);
    }

    private ComplianceReportItemResponse toItem(SelectedEventRow row) {
        AuditEvent event = row.event();
        return new ComplianceReportItemResponse(
                event.getId(),
                event.getActorId(),
                event.getResourceId(),
                event.getEventType(),
                TimeFormats.formatUtcMillis(event.getTimestamp()),
                event.getContentHash(),
                event.getRecordHash(),
                row.archived());
    }

    private Instant parseRequiredTimestamp(String value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        String validated = InputLimits.requireNonBlank(value, fieldName);
        try {
            return TimeFormats.parseInstant(validated);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid timestamp", exception);
        }
    }

    private Long parseAfterId(String value) {
        String validated = InputLimits.requireNonBlankIfPresent(value, "afterId");
        if (validated == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(validated);
            if (parsed < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "afterId must not be negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "afterId must be a valid number", exception);
        }
    }

    private int parseLimit(String value) {
        String validated = InputLimits.requireNonBlankIfPresent(value, "limit");
        if (validated == null) {
            return DEFAULT_LIMIT;
        }
        try {
            int parsed = Integer.parseInt(validated);
            if (parsed < 1 || parsed > MAX_LIMIT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 200");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be a valid number", exception);
        }
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
    }
}
