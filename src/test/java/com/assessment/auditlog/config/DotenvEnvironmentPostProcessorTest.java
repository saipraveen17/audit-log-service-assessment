package com.assessment.auditlog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void mapsEnvironmentStyleDotenvKeysToSpringProperties() {
        var properties = DotenvEnvironmentPostProcessor.parse(List.of(
                "# local development",
                "POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/auditlog",
                "AUDIT_REDACTION_MASTER_KEY_BASE64=\"base64-key\"",
                "AUDIT_EXPORT_SIGNING_KEY_ID=local-export-key",
                "AUDIT_SECURITY_USERS_0_USERNAME=audit-admin",
                "AUDIT_SECURITY_USERS_0_PASSWORD='{noop}secret'",
                "AUDIT_SECURITY_USERS_0_ROLES_0=AUDIT_ADMIN"));

        assertThat(properties)
                .containsEntry("POSTGRES_JDBC_URL", "jdbc:postgresql://localhost:5432/auditlog")
                .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5432/auditlog")
                .containsEntry("audit.redaction.master-key-base64", "base64-key")
                .containsEntry("audit.export.signing-key-id", "local-export-key")
                .containsEntry("audit.security.users[0].username", "audit-admin")
                .containsEntry("audit.security.users[0].password", "{noop}secret")
                .containsEntry("audit.security.users[0].roles[0]", "AUDIT_ADMIN");
    }

    @Test
    void loadsDotenvWithoutOverridingConfiguredOrRealEnvironmentProperties() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.write(dotenv, List.of(
                "AUDIT_REDACTION_MASTER_KEY_BASE64=from-dotenv",
                "AUDIT_EXPORT_SIGNING_KEY_ID=dotenv-export-key",
                "AUDIT_EXPORT_PRIVATE_KEY_BASE64=from-dotenv-private-key"));
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "applicationProperties",
                Map.of("audit.export.signing-key-id", "from-application-properties")));
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("audit.redaction.master-key-base64", "from-environment")));

        new DotenvEnvironmentPostProcessor(dotenv).postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("audit.redaction.master-key-base64")).isEqualTo("from-environment");
        assertThat(environment.getProperty("audit.export.signing-key-id")).isEqualTo("from-application-properties");
        assertThat(environment.getProperty("audit.export.private-key-base64")).isEqualTo("from-dotenv-private-key");
    }
}
