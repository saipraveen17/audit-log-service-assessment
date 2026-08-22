package com.assessment.auditlog.controller;

import java.net.URI;

import com.assessment.auditlog.dto.AuditEventQueryResponse;
import com.assessment.auditlog.dto.AuditEventResponse;
import com.assessment.auditlog.dto.CreateAuditEventRequest;
import com.assessment.auditlog.dto.RedactionRequest;
import com.assessment.auditlog.dto.RedactionResponse;
import com.assessment.auditlog.service.AuditEventService;
import com.assessment.auditlog.service.RedactionService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditEventController {

    private final AuditEventService auditEventService;

    private final RedactionService redactionService;

    public AuditEventController(AuditEventService auditEventService, RedactionService redactionService) {
        this.auditEventService = auditEventService;
        this.redactionService = redactionService;
    }

    @PostMapping("/audit/events")
    ResponseEntity<AuditEventResponse> create(@Valid @RequestBody CreateAuditEventRequest request) {
        AuditEventResponse response = auditEventService.create(request);
        return ResponseEntity
                .created(URI.create("/audit/events/" + response.id()))
                .body(response);
    }

    @GetMapping("/audit/events")
    AuditEventQueryResponse query(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String afterId,
            @RequestParam(required = false) String limit) {
        return auditEventService.query(actorId, resourceType, resourceId, eventType, from, to, afterId, limit);
    }

    @PostMapping("/audit/events/{id}/redactions")
    RedactionResponse redact(
            @PathVariable long id,
            @Valid @RequestBody RedactionRequest request,
            Authentication authentication) {
        return redactionService.redact(id, request, authentication.getName());
    }
}
