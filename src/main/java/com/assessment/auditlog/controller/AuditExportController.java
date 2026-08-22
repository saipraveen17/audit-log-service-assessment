package com.assessment.auditlog.controller;

import com.assessment.auditlog.dto.AuditExportBundle;
import com.assessment.auditlog.service.AuditExportService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditExportController {

    private final AuditExportService auditExportService;

    public AuditExportController(AuditExportService auditExportService) {
        this.auditExportService = auditExportService;
    }

    @GetMapping("/audit/exports")
    AuditExportBundle export(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId) {
        return auditExportService.export(actorId, resourceId);
    }
}
