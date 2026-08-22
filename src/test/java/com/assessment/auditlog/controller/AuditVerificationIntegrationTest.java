package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.service.AuditHashService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class AuditVerificationIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final Instant T1 = Instant.parse("2026-08-22T10:00:00.000Z");

    private static final Instant T2 = Instant.parse("2026-08-22T10:01:00.000Z");

    private static final Instant T3 = Instant.parse("2026-08-22T10:02:00.000Z");

    private static final String ZERO_HASH = "0".repeat(64);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdjustableClock testClock;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("delete from audit_sensitive_field_key");
        jdbcTemplate.update("delete from audit_event");
        jdbcTemplate.update(
                """
                        insert into audit_chain_state (name, last_id, last_record_hash)
                        values ('GLOBAL', 0, ?)
                        on conflict (name) do update
                        set last_id = 0, last_record_hash = excluded.last_record_hash
                        """,
                AuditHashService.GENESIS_HASH);
        testClock.setInstant(FIXED_INSTANT);
    }

    @Test
    void emptyChainIsIntact() throws Exception {
        mockMvc.perform(verifyAsVerifier())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.verifiedRecordCount").value(0))
                .andExpect(jsonPath("$.snapshotLastId").value(0))
                .andExpect(jsonPath("$.snapshotLastRecordHash").value(AuditHashService.GENESIS_HASH))
                .andExpect(jsonPath("$.firstInconsistentId").isEmpty())
                .andExpect(jsonPath("$.violationType").isEmpty());
    }

    @Test
    void validEventsProduceIntactResultWithoutPayloadContents() throws Exception {
        createThreeEvents();

        String response = mockMvc.perform(verifyAsVerifier())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.verifiedRecordCount").value(3))
                .andExpect(jsonPath("$.snapshotLastId").value(3))
                .andExpect(jsonPath("$.firstInconsistentId").isEmpty())
                .andExpect(jsonPath("$.violationType").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("CUSTOMER_SUPPORT");
    }

    @Test
    void changedActorIdDetectsContentHashMismatch() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set actor_id = ? where id = 2", "tampered-actor");

        assertBroken("CONTENT_HASH_MISMATCH", 2, 1);
    }

    @Test
    void changedPayloadDetectsContentHashMismatch() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set payload = ?::jsonb where id = 2", "{\"purpose\":\"TAMPERED\"}");

        assertBroken("CONTENT_HASH_MISMATCH", 2, 1);
    }

    @Test
    void changedPreviousHashDetectsPreviousHashMismatch() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set previous_hash = ? where id = 2", ZERO_HASH);

        assertBroken("PREVIOUS_HASH_MISMATCH", 2, 1);
    }

    @Test
    void changedRecordHashDetectsRecordHashMismatch() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set record_hash = ? where id = 2", ZERO_HASH);

        assertBroken("RECORD_HASH_MISMATCH", 2, 1);
    }

    @Test
    void changedHashVersionDetectsUnsupportedHashVersion() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set hash_version = ? where id = 2", 999);

        assertBroken("UNSUPPORTED_HASH_VERSION", 2, 1);
    }

    @Test
    void deletedMiddleEventDetectsIdGap() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("delete from audit_event where id = 2");

        assertBroken("ID_GAP", 2, 1);
    }

    @Test
    void chainStateExpectingMissingFinalIdReportsIdGap() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("delete from audit_event where id = 3");

        assertBroken("ID_GAP", 3, 2);
    }

    @Test
    void changedGlobalHeadHashDetectsChainHeadMismatch() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_chain_state set last_record_hash = ? where name = 'GLOBAL'", ZERO_HASH);

        assertBroken("CHAIN_HEAD_MISMATCH", 3, 3);
    }

    @Test
    void eventBeyondSnapshotLastIdDetectsChainHeadMismatch() throws Exception {
        createThreeEvents();
        String secondRecordHash = jdbcTemplate.queryForObject(
                "select record_hash from audit_event where id = 2",
                String.class);
        jdbcTemplate.update(
                "update audit_chain_state set last_id = 2, last_record_hash = ? where name = 'GLOBAL'",
                secondRecordHash);

        assertBroken("CHAIN_HEAD_MISMATCH", 3, 2);
    }

    @Test
    void earliestInconsistencyIsReportedWhenMultipleRecordsAreCorrupted() throws Exception {
        createThreeEvents();
        jdbcTemplate.update("update audit_event set actor_id = ? where id = 2", "tampered-actor");
        jdbcTemplate.update("update audit_event set previous_hash = ? where id = 3", ZERO_HASH);

        assertBroken("CONTENT_HASH_MISMATCH", 2, 1);
    }

    @Test
    void enforcesVerifyAuthorization() throws Exception {
        createThreeEvents();

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/verify").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/verify").with(user("verifier").roles("AUDIT_VERIFIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));

        mockMvc.perform(get("/audit/verify").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));
    }

    private void createThreeEvents() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-102", "CLIENT_ACCOUNT", "account-502", T2);
        createEvent("SESSION_STARTED", "employee-103", "SESSION", "session-701", T3);
    }

    private long createEvent(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            Instant timestamp) throws Exception {
        testClock.setInstant(timestamp);
        String body = """
                {
                  "eventType": "%s",
                  "actorId": "%s",
                  "resourceType": "%s",
                  "resourceId": "%s",
                  "payload": {"purpose": "CUSTOMER_SUPPORT"}
                }
                """.formatted(eventType, actorId, resourceType, resourceId);
        String response = mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void assertBroken(String violationType, long firstInconsistentId, long verifiedRecordCount)
            throws Exception {
        String response = mockMvc.perform(verifyAsVerifier())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(false))
                .andExpect(jsonPath("$.verifiedRecordCount").value(verifiedRecordCount))
                .andExpect(jsonPath("$.snapshotLastId").isNumber())
                .andExpect(jsonPath("$.snapshotLastRecordHash").isString())
                .andExpect(jsonPath("$.firstInconsistentId").value(firstInconsistentId))
                .andExpect(jsonPath("$.violationType").value(violationType))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("CUSTOMER_SUPPORT");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder verifyAsVerifier() {
        return get("/audit/verify").with(user("verifier").roles("AUDIT_VERIFIER"));
    }
}
