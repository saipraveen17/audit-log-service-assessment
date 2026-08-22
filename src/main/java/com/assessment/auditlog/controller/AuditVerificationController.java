package com.assessment.auditlog.controller;

import com.assessment.auditlog.dto.AuditVerificationResponse;
import com.assessment.auditlog.service.AuditVerificationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditVerificationController {

    private final AuditVerificationService auditVerificationService;

    public AuditVerificationController(AuditVerificationService auditVerificationService) {
        this.auditVerificationService = auditVerificationService;
    }

    @GetMapping("/audit/verify")
    AuditVerificationResponse verify() {
        return auditVerificationService.verify();
    }
}
