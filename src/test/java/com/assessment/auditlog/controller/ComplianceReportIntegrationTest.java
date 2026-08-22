package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.service.AuditHashService;
import com.assessment.auditlog.service.RetentionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class ComplianceReportIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final String FROM = "2026-08-22T10:00:00.000Z";

    private static final String TO = "2026-08-22T12:00:00.000Z";

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
        jdbcTemplate.update("delete from audit_archive_marker");
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
    void requiresFromAndToAndRejectsInvalidRangesAndParameters() throws Exception {
        mockMvc.perform(get("/audit/compliance/client-account-access")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("to", TO))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/audit/compliance/client-account-access")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("from", FROM))
                .andExpect(status().isBadRequest());

        mockMvc.perform(emptyReportRequest()
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("from", "not-a-time")
                        .param("to", TO))
                .andExpect(status().isBadRequest());

        mockMvc.perform(emptyReportRequest()
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("from", TO)
                        .param("to", FROM))
                .andExpect(status().isBadRequest());

        mockMvc.perform(reportRequest().param("actorId", " "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(reportRequest().param("afterId", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(reportRequest().param("limit", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(reportRequest().param("limit", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsOnlyClientAccountEventsInIdOrder() throws Exception {
        long firstId = createEvent(
                "CLIENT_ACCOUNT_VIEWED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}",
                null,
                Instant.parse("2026-08-22T10:10:00.000Z"));
        createEvent(
                "CUSTOMER_PROFILE_VIEWED",
                "employee-101",
                "CUSTOMER_PROFILE",
                "customer-501",
                "{\"purpose\":\"SUPPORT\"}",
                null,
                Instant.parse("2026-08-22T10:20:00.000Z"));
        long secondId = createEvent(
                "CLIENT_ACCOUNT_DENIED",
                "service:policy-checker",
                "CLIENT_ACCOUNT",
                "account-502",
                "{\"outcome\":\"DENIED\"}",
                null,
                Instant.parse("2026-08-22T10:30:00.000Z"));

        mockMvc.perform(reportRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(FROM))
                .andExpect(jsonPath("$.to").value(TO))
                .andExpect(jsonPath("$.snapshotLastId").value(3))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(firstId))
                .andExpect(jsonPath("$.items[1].id").value(secondId))
                .andExpect(jsonPath("$.items[0].resourceId").value("account-501"))
                .andExpect(jsonPath("$.items[1].actorId").value("service:policy-checker"))
                .andExpect(jsonPath("$.items[1].eventType").value("CLIENT_ACCOUNT_DENIED"))
                .andExpect(jsonPath("$.nextCursor").value(secondId))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void appliesActorResourceAndEventTypeFiltersIndependentlyAndTogether() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));
        createEvent("CLIENT_ACCOUNT_EXPORTED", "employee-101", "CLIENT_ACCOUNT", "account-502",
                "{\"purpose\":\"REVIEW\"}", null, Instant.parse("2026-08-22T10:20:00.000Z"));
        createEvent("CLIENT_ACCOUNT_VIEWED", "service:sync-worker", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SYNC\"}", null, Instant.parse("2026-08-22T10:30:00.000Z"));

        mockMvc.perform(reportRequest().param("actorId", "employee-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(reportRequest().param("resourceId", "account-501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(reportRequest().param("eventType", "CLIENT_ACCOUNT_EXPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].resourceId").value("account-502"));

        mockMvc.perform(reportRequest()
                        .param("actorId", "service:sync-worker")
                        .param("resourceId", "account-501")
                        .param("eventType", "CLIENT_ACCOUNT_VIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].actorId").value("service:sync-worker"));
    }

    @Test
    void usesInclusiveFromAndExclusiveToBoundaries() throws Exception {
        long atFromId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-from",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse(FROM));
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-102", "CLIENT_ACCOUNT", "account-to",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse(TO));

        mockMvc.perform(reportRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(atFromId));
    }

    @Test
    void includesArchivedHistoryAndPaginatesWithoutDuplicates() throws Exception {
        long firstId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));
        long secondId = createEvent("CLIENT_ACCOUNT_UPDATED", "employee-102", "CLIENT_ACCOUNT", "account-502",
                "{\"field\":\"status\"}", null, Instant.parse("2026-08-22T10:20:00.000Z"));
        long thirdId = createEvent("CLIENT_ACCOUNT_DENIED", "service:policy-checker", "CLIENT_ACCOUNT", "account-503",
                "{\"outcome\":\"DENIED\"}", null, Instant.parse("2026-08-22T10:30:00.000Z"));
        archiveEvent(firstId);

        JsonNode firstPage = performReport("limit", "2");
        assertThat(firstPage.get("hasMore").asBoolean()).isTrue();
        assertThat(firstPage.get("items").get(0).get("id").asLong()).isEqualTo(firstId);
        assertThat(firstPage.get("items").get(0).get("archived").asBoolean()).isTrue();
        assertThat(firstPage.get("items").get(1).get("id").asLong()).isEqualTo(secondId);

        JsonNode secondPage = performReport("afterId", firstPage.get("nextCursor").asText(), "limit", "2");
        assertThat(secondPage.get("hasMore").asBoolean()).isFalse();
        assertThat(secondPage.get("items")).hasSize(1);
        assertThat(secondPage.get("items").get(0).get("id").asLong()).isEqualTo(thirdId);

        List<Long> ids = List.of(
                firstPage.get("items").get(0).get("id").asLong(),
                firstPage.get("items").get(1).get("id").asLong(),
                secondPage.get("items").get(0).get("id").asLong());
        assertThat(new HashSet<>(ids)).hasSize(3);
        assertThat(ids).containsExactly(firstId, secondId, thirdId);
    }

    @Test
    void emptyResultHasNullCursorAndNoMoreRows() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));

        mockMvc.perform(reportRequest().param("actorId", "employee-404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void sensitiveAndRedactedEventsExposeOnlySafeMetadata() throws Exception {
        long eventId = createEvent(
                "CLIENT_ACCOUNT_VIEWED",
                "employee-101",
                "CLIENT_ACCOUNT",
                "account-501",
                "{\"accountNumber\":\"1234567890\",\"purpose\":\"CUSTOMER_SUPPORT\"}",
                "[\"/accountNumber\"]",
                Instant.parse("2026-08-22T10:10:00.000Z"));
        redact(eventId);

        String response = mockMvc.perform(reportRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(eventId))
                .andExpect(jsonPath("$.items[0].contentHash").isString())
                .andExpect(jsonPath("$.items[0].recordHash").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain(
                "payload",
                "committedPayload",
                "1234567890",
                "CUSTOMER_SUPPORT",
                "ciphertext",
                "_encrypted",
                "wrappedKey",
                "wrappingIv",
                "redactionReason",
                "redactedBy");
    }

    @Test
    void brokenSourceChainReturnsConflict() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));
        jdbcTemplate.update("update audit_event set actor_id = ? where id = 1", "tampered-actor");

        mockMvc.perform(reportRequest())
                .andExpect(status().isConflict());
    }

    @Test
    void appendAfterReportCreationDoesNotInvalidateExistingSnapshotEvidence() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));
        JsonNode firstReport = performReport();

        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-102", "CLIENT_ACCOUNT", "account-502",
                "{\"purpose\":\"REVIEW\"}", null, Instant.parse("2026-08-22T10:20:00.000Z"));

        assertThat(firstReport.get("snapshotLastId").asLong()).isEqualTo(1);
        assertThat(firstReport.get("items")).hasSize(1);
        assertThat(firstReport.get("items").get(0).get("id").asLong()).isEqualTo(1);
    }

    @Test
    void enforcesComplianceAuthorization() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"SUPPORT\"}", null, Instant.parse("2026-08-22T10:10:00.000Z"));

        mockMvc.perform(get("/audit/compliance/client-account-access")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(emptyReportRequest()
                        .with(user("reader").roles("AUDIT_READER"))
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isForbidden());

        mockMvc.perform(emptyReportRequest()
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk());

        mockMvc.perform(reportRequest())
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder reportRequest() {
        return emptyReportRequest()
                .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                .param("from", FROM)
                .param("to", TO);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder emptyReportRequest() {
        return get("/audit/compliance/client-account-access");
    }

    private JsonNode performReport(String... extraParams) throws Exception {
        var request = reportRequest();
        for (int i = 0; i < extraParams.length; i += 2) {
            request.param(extraParams[i], extraParams[i + 1]);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private long createEvent(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payload,
            String sensitivePaths,
            Instant timestamp) throws Exception {
        testClock.setInstant(timestamp);
        String optionalSensitivePaths = sensitivePaths == null ? "" : ",\"sensitivePaths\":" + sensitivePaths;
        String response = mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "%s",
                                  "actorId": "%s",
                                  "resourceType": "%s",
                                  "resourceId": "%s",
                                  "payload": %s%s
                                }
                                """.formatted(eventType, actorId, resourceType, resourceId, payload, optionalSensitivePaths)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void archiveEvent(long eventId) {
        jdbcTemplate.update(
                "insert into audit_archive_marker (audit_event_id, archived_at, retention_days, reason) values (?, ?, ?, ?)",
                eventId,
                Timestamp.from(Instant.parse("2026-08-22T11:00:00.000Z")),
                90,
                RetentionService.RETENTION_REASON);
    }

    private void redact(long eventId) throws Exception {
        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/accountNumber"],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
