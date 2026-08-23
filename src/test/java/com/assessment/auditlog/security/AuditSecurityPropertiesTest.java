package com.assessment.auditlog.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AuditSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void acceptsValidConfiguredUsers() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username=audit-reader",
                        "audit.security.users[0].password={noop}secret",
                        "audit.security.users[0].roles[0]=AUDIT_READER",
                        "audit.security.users[1].username=audit-admin",
                        "audit.security.users[1].password={noop}secret",
                        "audit.security.users[1].roles[0]=AUDIT_ADMIN")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsBlankUsername() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username= ",
                        "audit.security.users[0].password={noop}secret",
                        "audit.security.users[0].roles[0]=AUDIT_READER")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankPassword() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username=audit-reader",
                        "audit.security.users[0].password= ",
                        "audit.security.users[0].roles[0]=AUDIT_READER")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsEmptyRoles() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username=audit-reader",
                        "audit.security.users[0].password={noop}secret")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsDuplicateUsernames() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username=audit-reader",
                        "audit.security.users[0].password={noop}secret",
                        "audit.security.users[0].roles[0]=AUDIT_READER",
                        "audit.security.users[1].username=audit-reader",
                        "audit.security.users[1].password={noop}secret",
                        "audit.security.users[1].roles[0]=AUDIT_ADMIN")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsUnsupportedRoles() {
        contextRunner
                .withPropertyValues(
                        "audit.security.users[0].username=audit-reader",
                        "audit.security.users[0].password={noop}secret",
                        "audit.security.users[0].roles[0]=ROOT")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(AuditSecurityProperties.class)
    static class TestConfiguration {
    }
}
