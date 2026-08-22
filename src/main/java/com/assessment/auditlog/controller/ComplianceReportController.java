package com.assessment.auditlog.controller;

import com.assessment.auditlog.dto.ComplianceReportResponse;
import com.assessment.auditlog.service.ComplianceReportService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;

    public ComplianceReportController(ComplianceReportService complianceReportService) {
        this.complianceReportService = complianceReportService;
    }

    @GetMapping("/audit/compliance/client-account-access")
    ComplianceReportResponse clientAccountAccess(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String afterId,
            @RequestParam(required = false) String limit) {
        return complianceReportService.report(from, to, actorId, resourceId, eventType, afterId, limit);
    }
}
