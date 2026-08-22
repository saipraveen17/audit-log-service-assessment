package com.assessment.auditlog.controller;

import java.net.URI;

import com.assessment.auditlog.dto.AuditEventResponse;
import com.assessment.auditlog.dto.CreateAuditEventRequest;
import com.assessment.auditlog.service.AuditEventService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping("/audit/events")
    ResponseEntity<AuditEventResponse> create(@Valid @RequestBody CreateAuditEventRequest request) {
        AuditEventResponse response = auditEventService.create(request);
        return ResponseEntity
                .created(URI.create("/audit/events/" + response.id()))
                .body(response);
    }
}
