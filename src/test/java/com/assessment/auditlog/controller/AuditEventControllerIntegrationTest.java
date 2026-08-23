package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.entity.AuditEvent;
import com.assessment.auditlog.repository.AuditEventRepository;
import com.assessment.auditlog.service.AuditHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class AuditEventControllerIntegrationTest extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditHashService auditHashService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    }

    @Test
    void createsFirstAndSecondEventsWithLinkedHashes() throws Exception {
        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": {"purpose": "CUSTOMER_SUPPORT"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/audit/events/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.previousHash").value(AuditHashService.GENESIS_HASH))
                .andExpect(jsonPath("$.hashVersion").value(1))
                .andExpect(jsonPath("$.timestamp").value("2026-08-22T10:15:30.123Z"));

        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLIENT_ACCOUNT_UPDATED",
                                  "actorId": "employee-102",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-502",
                                  "payload": {"field": "status"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/audit/events/2"))
                .andExpect(jsonPath("$.id").value(2));

        List<AuditEvent> events = auditEventRepository.findAllByOrderByIdAsc();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getId()).isEqualTo(1);
        assertThat(events.get(1).getId()).isEqualTo(2);
        assertThat(events.get(0).getPreviousHash()).isEqualTo(AuditHashService.GENESIS_HASH);
        assertThat(events.get(1).getPreviousHash()).isEqualTo(events.get(0).getRecordHash());

        AuditEvent first = events.get(0);
        AuditEvent second = events.get(1);
        assertThat(first.getContentHash()).isEqualTo(auditHashService.contentHash(
                first.getEventType(),
                first.getActorId(),
                first.getResourceType(),
                first.getResourceId(),
                first.getCommittedPayload(),
                first.getTimestamp()));
        assertThat(first.getRecordHash()).isEqualTo(auditHashService.recordHash(
                first.getHashVersion(),
                first.getId(),
                first.getContentHash(),
                first.getPreviousHash()));
        assertThat(second.getContentHash()).isEqualTo(auditHashService.contentHash(
                second.getEventType(),
                second.getActorId(),
                second.getResourceType(),
                second.getResourceId(),
                second.getCommittedPayload(),
                second.getTimestamp()));
        assertThat(second.getRecordHash()).isEqualTo(auditHashService.recordHash(
                second.getHashVersion(),
                second.getId(),
                second.getContentHash(),
                second.getPreviousHash()));

        Map<String, Object> chainState = jdbcTemplate.queryForMap(
                "select last_id, last_record_hash from audit_chain_state where name = 'GLOBAL'");
        assertThat(((Number) chainState.get("last_id")).longValue()).isEqualTo(2);
        assertThat(chainState.get("last_record_hash")).isEqualTo(second.getRecordHash());
    }

    @Test
    void concurrentAppendsProduceUniqueContiguousLinkedIds() throws Exception {
        int count = 12;
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = IntStream.rangeClosed(1, count)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        mockMvc.perform(post("/audit/events")
                                        .with(user("writer").roles("AUDIT_WRITER"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                                                  "actorId": "employee-%d",
                                                  "resourceType": "CLIENT_ACCOUNT",
                                                  "resourceId": "account-%d",
                                                  "payload": {"sequence": %d}
                                                }
                                                """.formatted(index, index, index)))
                                .andExpect(status().isCreated());
                        return null;
                    })
                    .toList();
            for (var future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        List<AuditEvent> events = auditEventRepository.findAllByOrderByIdAsc().stream()
                .sorted(Comparator.comparing(AuditEvent::getId))
                .toList();
        assertThat(events).hasSize(count);
        assertThat(events.stream().map(AuditEvent::getId)).containsExactlyElementsOf(
                IntStream.rangeClosed(1, count).mapToObj(Long::valueOf).toList());
        assertThat(new HashSet<>(events.stream().map(AuditEvent::getId).toList())).hasSize(count);
        assertThat(events.get(0).getPreviousHash()).isEqualTo(AuditHashService.GENESIS_HASH);
        for (int i = 1; i < events.size(); i++) {
            assertThat(events.get(i).getPreviousHash()).isEqualTo(events.get(i - 1).getRecordHash());
        }
    }

    @Test
    void rejectsBlankFieldsAndNonObjectPayloads() throws Exception {
        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": " ",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": {"purpose": "CUSTOMER_SUPPORT"}
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": ["not", "object"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOverlongCreateFieldsWithoutEchoingPayloadValues() throws Exception {
        String longValue = "x".repeat(256);

        String response = mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "%s",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": {"secret": "SHOULD_NOT_LEAK"}
                                }
                                """.formatted(longValue)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("SHOULD_NOT_LEAK", longValue);
    }

    @Test
    void rejectsUnsupportedRequestProperties() throws Exception {
        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": {"purpose": "CUSTOMER_SUPPORT"},
                                  "timestamp": "2026-08-22T10:15:30.123Z"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                                  "actorId": "employee-101",
                                  "resourceType": "CLIENT_ACCOUNT",
                                  "resourceId": "account-501",
                                  "payload": {"purpose": "CUSTOMER_SUPPORT"},
                                  "unsupported": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesEndpointAuthorization() throws Exception {
        String request = """
                {
                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                  "actorId": "employee-101",
                  "resourceType": "CLIENT_ACCOUNT",
                  "resourceId": "account-501",
                  "payload": {"purpose": "CUSTOMER_SUPPORT"}
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/audit/events")
                        .with(user("reader").roles("AUDIT_READER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/audit/events")
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }
}
