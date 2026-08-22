package com.assessment.auditlog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.assessment.auditlog.dto.AuditEventQueryItemResponse;
import com.assessment.auditlog.dto.AuditEventQueryResponse;
import com.assessment.auditlog.dto.AuditEventResponse;
import com.assessment.auditlog.dto.CreateAuditEventRequest;
import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditChainStateRepository;
import com.assessment.auditlog.repository.AuditEventInsertRepository;
import com.assessment.auditlog.repository.AuditEventQueryRepository;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditEventService {

    private static final int DEFAULT_QUERY_LIMIT = 50;

    private static final int MAX_QUERY_LIMIT = 200;

    private final Clock clock;

    private final AuditHashService auditHashService;

    private final AuditChainStateRepository chainStateRepository;

    private final AuditEventInsertRepository auditEventInsertRepository;

    private final AuditEventQueryRepository auditEventQueryRepository;

    public AuditEventService(
            Clock clock,
            AuditHashService auditHashService,
            AuditChainStateRepository chainStateRepository,
            AuditEventInsertRepository auditEventInsertRepository,
            AuditEventQueryRepository auditEventQueryRepository) {
        this.clock = clock;
        this.auditHashService = auditHashService;
        this.chainStateRepository = chainStateRepository;
        this.auditEventInsertRepository = auditEventInsertRepository;
        this.auditEventQueryRepository = auditEventQueryRepository;
    }

    @Transactional
    public AuditEventResponse create(CreateAuditEventRequest request) {
        JsonNode committedPayload = validatePayload(request.payload());
        Instant timestamp = TimeFormats.truncateToMillis(clock.instant());
        String contentHash = auditHashService.contentHash(
                request.eventType(),
                request.actorId(),
                request.resourceType(),
                request.resourceId(),
                committedPayload,
                timestamp);

        AuditChainState chainState = chainStateRepository.findByNameForUpdate(AuditChainState.GLOBAL_NAME)
                .orElseThrow(() -> new IllegalStateException("Audit chain state is not initialized"));
        long id = chainState.getLastId() + 1;
        String previousHash = chainState.getLastRecordHash();
        String recordHash = auditHashService.recordHash(AuditHashService.HASH_VERSION, id, contentHash, previousHash);

        AuditEvent event = new AuditEvent(
                id,
                request.eventType(),
                request.actorId(),
                request.resourceType(),
                request.resourceId(),
                committedPayload,
                timestamp,
                contentHash,
                previousHash,
                recordHash,
                AuditHashService.HASH_VERSION);
        auditEventInsertRepository.insert(event);
        chainState.advanceTo(id, recordHash);
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public AuditEventQueryResponse query(
            String actorId,
            String resourceType,
            String resourceId,
            String eventType,
            String from,
            String to,
            String afterId,
            String limit) {
        AuditEventQuery query = new AuditEventQuery(
                requireNonBlankIfPresent(actorId, "actorId"),
                requireNonBlankIfPresent(resourceType, "resourceType"),
                requireNonBlankIfPresent(resourceId, "resourceId"),
                requireNonBlankIfPresent(eventType, "eventType"),
                parseOptionalTimestamp(from, "from"),
                parseOptionalTimestamp(to, "to"),
                parseAfterId(afterId),
                parseLimit(limit));
        validateTimeRange(query.from(), query.to());

        List<AuditEvent> fetchedEvents = auditEventQueryRepository.find(query, query.limit() + 1);
        boolean hasMore = fetchedEvents.size() > query.limit();
        List<AuditEventQueryItemResponse> items = fetchedEvents.stream()
                .limit(query.limit())
                .map(this::toQueryItemResponse)
                .toList();
        Long nextCursor = items.isEmpty() ? null : items.getLast().id();
        return new AuditEventQueryResponse(items, nextCursor, hasMore);
    }

    private JsonNode validatePayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload must be a JSON object.");
        }
        return payload.deepCopy();
    }

    private String requireNonBlankIfPresent(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }

    private Instant parseOptionalTimestamp(String value, String fieldName) {
        String validated = requireNonBlankIfPresent(value, fieldName);
        if (validated == null) {
            return null;
        }
        try {
            return TimeFormats.parseInstant(validated);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid timestamp", exception);
        }
    }

    private Long parseAfterId(String value) {
        String validated = requireNonBlankIfPresent(value, "afterId");
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
        String validated = requireNonBlankIfPresent(value, "limit");
        if (validated == null) {
            return DEFAULT_QUERY_LIMIT;
        }
        try {
            int parsed = Integer.parseInt(validated);
            if (parsed < 1 || parsed > MAX_QUERY_LIMIT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 200");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be a valid number", exception);
        }
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActorId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCommittedPayload(),
                TimeFormats.formatUtcMillis(event.getTimestamp()),
                event.getContentHash(),
                event.getPreviousHash(),
                event.getRecordHash(),
                event.getHashVersion(),
                false);
    }

    private AuditEventQueryItemResponse toQueryItemResponse(AuditEvent event) {
        return new AuditEventQueryItemResponse(
                event.getId(),
                event.getEventType(),
                event.getActorId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCommittedPayload(),
                TimeFormats.formatUtcMillis(event.getTimestamp()),
                event.getRecordHash(),
                false);
    }
}
