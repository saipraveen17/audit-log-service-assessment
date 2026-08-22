package com.assessment.auditlog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.assessment.auditlog.dto.RedactionRequest;
import com.assessment.auditlog.dto.RedactionResponse;
import com.assessment.auditlog.entity.AuditSensitiveFieldKey;
import com.assessment.auditlog.repository.AuditEventRepository;
import com.assessment.auditlog.repository.AuditSensitiveFieldKeyRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RedactionService {

    private final Clock clock;

    private final AuditEventRepository auditEventRepository;

    private final AuditSensitiveFieldKeyRepository sensitiveFieldKeyRepository;

    private final SensitivePayloadService sensitivePayloadService;

    public RedactionService(
            Clock clock,
            AuditEventRepository auditEventRepository,
            AuditSensitiveFieldKeyRepository sensitiveFieldKeyRepository,
            SensitivePayloadService sensitivePayloadService) {
        this.clock = clock;
        this.auditEventRepository = auditEventRepository;
        this.sensitiveFieldKeyRepository = sensitiveFieldKeyRepository;
        this.sensitivePayloadService = sensitivePayloadService;
    }

    @Transactional
    public RedactionResponse redact(long eventId, RedactionRequest request, String redactedBy) {
        if (eventId < 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit event was not found");
        }
        auditEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit event was not found"));
        List<String> paths = sensitivePayloadService.validateRedactionPaths(request.paths());
        if (paths.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paths must not be empty");
        }

        List<AuditSensitiveFieldKey> keys = sensitiveFieldKeyRepository.findByAuditEventIdAndJsonPointerIn(eventId, paths);
        Map<String, AuditSensitiveFieldKey> keysByPath = keys.stream()
                .collect(Collectors.toMap(AuditSensitiveFieldKey::getJsonPointer, Function.identity()));
        if (!keysByPath.keySet().containsAll(paths)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Path was not declared sensitive");
        }

        Instant redactedAt = TimeFormats.truncateToMillis(clock.instant());
        List<String> redactedPaths = new ArrayList<>();
        List<String> alreadyRedactedPaths = new ArrayList<>();
        for (String path : paths) {
            AuditSensitiveFieldKey key = keysByPath.get(path);
            if (key.isRedacted()) {
                alreadyRedactedPaths.add(path);
            } else {
                key.redact(redactedAt, request.reason(), redactedBy);
                redactedPaths.add(path);
            }
        }
        return new RedactionResponse(eventId, redactedPaths, alreadyRedactedPaths, false);
    }
}
