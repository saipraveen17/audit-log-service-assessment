package com.assessment.auditlog.service;

import com.assessment.auditlog.entity.AuditChainState;
import com.assessment.auditlog.repository.AuditChainStateRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class AuditChainStateInitializer {

    @Bean
    ApplicationRunner initializeAuditChainState(AuditChainStateInitializationService service) {
        return args -> service.initialize();
    }

    public static class AuditChainStateInitializationService {

        private final AuditChainStateRepository repository;

        public AuditChainStateInitializationService(AuditChainStateRepository repository) {
            this.repository = repository;
        }

        @Transactional
        public void initialize() {
            if (!repository.existsById(AuditChainState.GLOBAL_NAME)) {
                repository.save(new AuditChainState(AuditChainState.GLOBAL_NAME, 0, AuditHashService.GENESIS_HASH));
            }
        }
    }

    @Bean
    AuditChainStateInitializationService auditChainStateInitializationService(AuditChainStateRepository repository) {
        return new AuditChainStateInitializationService(repository);
    }
}
