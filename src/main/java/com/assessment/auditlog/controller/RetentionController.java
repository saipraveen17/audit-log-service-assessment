package com.assessment.auditlog.controller;

import com.assessment.auditlog.dto.RetentionRunResponse;
import com.assessment.auditlog.service.RetentionService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetentionController {

    private final RetentionService retentionService;

    public RetentionController(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @PostMapping("/audit/retention/run")
    RetentionRunResponse runRetention() {
        return retentionService.runRetention();
    }
}
