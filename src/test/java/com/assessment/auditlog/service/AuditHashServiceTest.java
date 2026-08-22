package com.assessment.auditlog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuditHashServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AuditHashService hashService = new AuditHashService(new JsonCanonicalizer(objectMapper));

    @Test
    void hashingIsDeterministicForSameInputs() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"action\":\"view\",\"nested\":{\"b\":2,\"a\":1}}");
        Instant timestamp = Instant.parse("2026-08-22T10:15:30.123456Z");

        String first = hashService.contentHash(
                "CLIENT_ACCOUNT_VIEWED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);
        String second = hashService.contentHash(
                "CLIENT_ACCOUNT_VIEWED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);

        assertThat(first).isEqualTo(second);
        assertThat(first).matches("[0-9a-f]{64}");
    }

    @Test
    void changedFieldChangesContentHash() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"action\":\"view\"}");
        Instant timestamp = Instant.parse("2026-08-22T10:15:30.123Z");

        String original = hashService.contentHash(
                "CLIENT_ACCOUNT_VIEWED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);
        String changed = hashService.contentHash(
                "CLIENT_ACCOUNT_UPDATED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);

        assertThat(changed).isNotEqualTo(original);
    }

    @Test
    void contentHashDoesNotCollideWhenCallerStringsContainOldDelimiters() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"action\":\"view\"}");
        Instant timestamp = Instant.parse("2026-08-22T10:15:30.123Z");

        String first = hashService.contentHash(
                "x\nactorId=y",
                "z",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);
        String second = hashService.contentHash(
                "x",
                "y\nactorId=z",
                "CLIENT_ACCOUNT",
                "account-501",
                payload,
                timestamp);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void recordHashIsLowercaseHexAndDependsOnEventId() {
        String first = hashService.recordHash(1, 1, "a".repeat(64), AuditHashService.GENESIS_HASH);
        String second = hashService.recordHash(1, 2, "a".repeat(64), AuditHashService.GENESIS_HASH);

        assertThat(first).matches("[0-9a-f]{64}");
        assertThat(second).isNotEqualTo(first);
    }
}
