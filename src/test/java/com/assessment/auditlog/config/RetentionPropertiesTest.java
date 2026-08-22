package com.assessment.auditlog.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

class RetentionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void defaultsRetentionDaysToNinety() {
        contextRunner.run(context -> assertThat(context.getBean(RetentionProperties.class).getRetentionDays()).isEqualTo(90));
    }

    @Test
    void rejectsNonPositiveRetentionDays() {
        contextRunner
                .withPropertyValues("audit.retention-days=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(RetentionProperties.class)
    static class TestConfiguration {
    }
}
