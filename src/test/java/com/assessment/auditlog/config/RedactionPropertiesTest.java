package com.assessment.auditlog.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RedactionPropertiesTest {

    private static final String VALID_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void acceptsValidBase64EncodedThirtyTwoByteMasterKey() {
        contextRunner
                .withPropertyValues("audit.redaction.master-key-base64=" + VALID_KEY)
                .run(context -> assertThat(context.getBean(RedactionProperties.class).masterKey()).hasSize(32));
    }

    @Test
    void rejectsMissingMasterKey() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInvalidMasterKey() {
        contextRunner
                .withPropertyValues("audit.redaction.master-key-base64=not-base64")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsWrongSizedMasterKey() {
        contextRunner
                .withPropertyValues("audit.redaction.master-key-base64=AA==")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(RedactionProperties.class)
    static class TestConfiguration {
    }
}
