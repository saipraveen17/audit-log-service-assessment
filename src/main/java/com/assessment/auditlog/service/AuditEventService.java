package com.assessment.auditlog.service;

import java.time.Clock;
import java.time.Instant;

import com.assessment.auditlog.dto.AuditEventResponse;
import com.assessment.auditlog.dto.CreateAuditEventRequest;
import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditChainStateRepository;
import com.assessment.auditlog.repository.AuditEventInsertRepository;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditEventService {

    private final Clock clock;

    private final AuditHashService auditHashService;

    private final AuditChainStateRepository chainStateRepository;

    private final AuditEventInsertRepository auditEventInsertRepository;

    public AuditEventService(
            Clock clock,
            AuditHashService auditHashService,
            AuditChainStateRepository chainStateRepository,
            AuditEventInsertRepository auditEventInsertRepository) {
        this.clock = clock;
        this.auditHashService = auditHashService;
        this.chainStateRepository = chainStateRepository;
        this.auditEventInsertRepository = auditEventInsertRepository;
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

    private JsonNode validatePayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload must be a JSON object.");
        }
        return payload.deepCopy();
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
}
