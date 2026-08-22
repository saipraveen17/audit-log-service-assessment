package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.service.AuditHashService;
import com.assessment.auditlog.service.RetentionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class RetentionIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final Instant RUN_TIME = Instant.parse("2026-08-22T10:00:00.000Z");

    private static final Instant CUTOFF = Instant.parse("2026-05-24T10:00:00.000Z");

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
    void archivesOnlyEventsBeforeCutoffAndIsIdempotent() throws Exception {
        long oldEventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "account-old", CUTOFF.minusMillis(1));
        long cutoffEventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-102", "account-cutoff", CUTOFF);
        long newEventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-103", "account-new", CUTOFF.plusMillis(1));
        List<Map<String, Object>> eventRowsBefore = rowsForEvents();
        Map<String, Object> chainStateBefore = chainState();

        testClock.setInstant(RUN_TIME);
        mockMvc.perform(post("/audit/retention/run").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionDays").value(90))
                .andExpect(jsonPath("$.archivedCount").value(1))
                .andExpect(jsonPath("$.alreadyArchivedCount").value(0));

        List<Long> archivedIds = jdbcTemplate.queryForList(
                "select audit_event_id from audit_archive_marker order by audit_event_id",
                Long.class);
        assertThat(archivedIds).containsExactly(oldEventId);
        Map<String, Object> marker = jdbcTemplate.queryForMap(
                "select retention_days, reason from audit_archive_marker where audit_event_id = ?",
                oldEventId);
        assertThat(marker.get("retention_days")).isEqualTo(90);
        assertThat(marker.get("reason")).isEqualTo(RetentionService.RETENTION_REASON);

        assertThat(rowsForEvents()).isEqualTo(eventRowsBefore);
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_event", Long.class)).isEqualTo(3);
        assertThat(chainState()).isEqualTo(chainStateBefore);

        mockMvc.perform(get("/audit/events")
                        .with(user("reader").roles("AUDIT_READER"))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(cutoffEventId))
                .andExpect(jsonPath("$.items[1].id").value(newEventId))
                .andExpect(jsonPath("$.items[0].archived").value(false))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/audit/verify").with(user("verifier").roles("AUDIT_VERIFIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.verifiedRecordCount").value(3))
                .andExpect(jsonPath("$.snapshotLastId").value(3));

        mockMvc.perform(post("/audit/retention/run").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionDays").value(90))
                .andExpect(jsonPath("$.archivedCount").value(0))
                .andExpect(jsonPath("$.alreadyArchivedCount").value(1));

        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_archive_marker", Long.class)).isEqualTo(1);
    }

    @Test
    void reportsExistingArchivedMarkersAndNewMarkersInSameRun() throws Exception {
        long firstOldEventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "account-old-1", CUTOFF.minusMillis(2));
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-102", "account-old-2", CUTOFF.minusMillis(1));
        jdbcTemplate.update(
                "insert into audit_archive_marker (audit_event_id, archived_at, retention_days, reason) values (?, ?, ?, ?)",
                firstOldEventId,
                Timestamp.from(CUTOFF),
                90,
                RetentionService.RETENTION_REASON);

        testClock.setInstant(RUN_TIME);
        mockMvc.perform(post("/audit/retention/run").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedCount").value(1))
                .andExpect(jsonPath("$.alreadyArchivedCount").value(1));
    }

    @Test
    void enforcesRetentionAuthorization() throws Exception {
        mockMvc.perform(post("/audit/retention/run"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/audit/retention/run").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/audit/retention/run").with(user("writer").roles("AUDIT_WRITER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/audit/retention/run").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk());
    }

    private long createEvent(String eventType, String actorId, String resourceId, Instant timestamp) throws Exception {
        testClock.setInstant(timestamp);
        String body = """
                {
                  "eventType": "%s",
                  "actorId": "%s",
                  "resourceType": "CLIENT_ACCOUNT",
                  "resourceId": "%s",
                  "payload": {"purpose": "CUSTOMER_SUPPORT"}
                }
                """.formatted(eventType, actorId, resourceId);
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

    private List<Map<String, Object>> rowsForEvents() {
        return jdbcTemplate.queryForList(
                """
                        select id, event_type, actor_id, resource_type, resource_id,
                               payload::text as payload, content_hash, previous_hash,
                               record_hash, hash_version
                        from audit_event
                        order by id
                        """);
    }

    private Map<String, Object> chainState() {
        return jdbcTemplate.queryForMap(
                "select last_id, last_record_hash from audit_chain_state where name = 'GLOBAL'");
    }
}
