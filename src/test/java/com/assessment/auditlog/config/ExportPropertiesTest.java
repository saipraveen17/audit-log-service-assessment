package com.assessment.auditlog.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ExportPropertiesTest {

    static final String VALID_PRIVATE_KEY =
            "MC4CAQAwBQYDK2VwBCIEIM0KZe1m+Najn7X3QadGxdEFLWlgFdbI1U3bdlGiboCG";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void acceptsValidEd25519Pkcs8PrivateKey() {
        contextRunner
                .withPropertyValues(
                        "audit.export.signing-key-id=test-export-key-1",
                        "audit.export.private-key-base64=" + VALID_PRIVATE_KEY)
                .run(context -> assertThat(context.getBean(ExportProperties.class).privateKey().getAlgorithm())
                        .isEqualTo("EdDSA"));
    }

    @Test
    void rejectsMissingSigningKeyId() {
        contextRunner
                .withPropertyValues("audit.export.private-key-base64=" + VALID_PRIVATE_KEY)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsMissingPrivateKey() {
        contextRunner
                .withPropertyValues("audit.export.signing-key-id=test-export-key-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInvalidPrivateKey() {
        contextRunner
                .withPropertyValues(
                        "audit.export.signing-key-id=test-export-key-1",
                        "audit.export.private-key-base64=not-base64")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ExportProperties.class)
    static class TestConfiguration {
    }
}
