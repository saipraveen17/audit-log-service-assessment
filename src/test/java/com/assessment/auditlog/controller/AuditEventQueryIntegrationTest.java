package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.service.AuditHashService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuditEventQueryIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final Instant T1 = Instant.parse("2026-08-22T10:00:00.000Z");

    private static final Instant T2 = Instant.parse("2026-08-22T10:01:00.000Z");

    private static final Instant T3 = Instant.parse("2026-08-22T10:02:00.000Z");

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
    void noFilterQueryReturnsEventsOrderedById() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T2);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-102", "CLIENT_ACCOUNT", "account-502", T1);
        createEvent("SESSION_STARTED", "employee-103", "SESSION", "session-701", T3);

        mockMvc.perform(queryAsReader())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[2].id").value(3))
                .andExpect(jsonPath("$.items[0].payload.purpose").value("CUSTOMER_SUPPORT"))
                .andExpect(jsonPath("$.items[0].archived").value(false))
                .andExpect(jsonPath("$.items[0].recordHash").isString())
                .andExpect(jsonPath("$.nextCursor").value(3))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void filtersByEachSupportedField() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-102", "CLIENT_ACCOUNT", "account-502", T2);
        createEvent("SESSION_STARTED", "employee-101", "SESSION", "session-701", T3);

        assertQueryIds(List.of(1L, 3L), "actorId", "employee-101");
        assertQueryIds(List.of(1L, 2L), "resourceType", "CLIENT_ACCOUNT");
        assertQueryIds(List.of(2L), "resourceId", "account-502");
        assertQueryIds(List.of(1L), "eventType", "CLIENT_ACCOUNT_VIEWED");
    }

    @Test
    void combinesMultipleFilters() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-101", "CLIENT_ACCOUNT", "account-501", T2);
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-102", "CLIENT_ACCOUNT", "account-501", T3);

        assertQueryIds(
                List.of(1L),
                "actorId", "employee-101",
                "resourceType", "CLIENT_ACCOUNT",
                "resourceId", "account-501",
                "eventType", "CLIENT_ACCOUNT_VIEWED");
    }

    @Test
    void appliesInclusiveFromAndExclusiveTo() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-502", T2);
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-503", T3);

        assertQueryIds(
                List.of(2L),
                "from", "2026-08-22T10:01:00.000Z",
                "to", "2026-08-22T10:02:00.000Z");
    }

    @Test
    void cursorPaginationDoesNotDuplicateOrSkipEvents() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createEvent("CLIENT_ACCOUNT_VIEWED", "employee-%d".formatted(i), "CLIENT_ACCOUNT", "account-%d".formatted(i), T1.plusSeconds(i));
        }

        JsonNode firstPage = queryJson("limit", "2");
        assertIds(firstPage, List.of(1L, 2L));
        assertThat(firstPage.get("nextCursor").asLong()).isEqualTo(2);
        assertThat(firstPage.get("hasMore").asBoolean()).isTrue();

        JsonNode secondPage = queryJson("afterId", "2", "limit", "2");
        assertIds(secondPage, List.of(3L, 4L));
        assertThat(secondPage.get("nextCursor").asLong()).isEqualTo(4);
        assertThat(secondPage.get("hasMore").asBoolean()).isTrue();

        JsonNode thirdPage = queryJson("afterId", "4", "limit", "2");
        assertIds(thirdPage, List.of(5L));
        assertThat(thirdPage.get("nextCursor").asLong()).isEqualTo(5);
        assertThat(thirdPage.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void emptyResultsReturnNullCursorAndNoMoreFlag() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);

        mockMvc.perform(queryAsReader("actorId", "missing-actor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void appliesDefaultAndValidLimits() throws Exception {
        for (int i = 1; i <= 51; i++) {
            createEvent("CLIENT_ACCOUNT_VIEWED", "employee-%d".formatted(i), "CLIENT_ACCOUNT", "account-%d".formatted(i), T1.plusSeconds(i));
        }

        JsonNode defaultPage = queryJson();
        assertThat(defaultPage.get("items")).hasSize(50);
        assertThat(defaultPage.get("nextCursor").asLong()).isEqualTo(50);
        assertThat(defaultPage.get("hasMore").asBoolean()).isTrue();

        JsonNode limitedPage = queryJson("limit", "1");
        assertIds(limitedPage, List.of(1L));
        assertThat(limitedPage.get("nextCursor").asLong()).isEqualTo(1);
        assertThat(limitedPage.get("hasMore").asBoolean()).isTrue();

        JsonNode maxPage = queryJson("limit", "200");
        assertThat(maxPage.get("items")).hasSize(51);
        assertThat(maxPage.get("nextCursor").asLong()).isEqualTo(51);
        assertThat(maxPage.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void rejectsInvalidQueryParameters() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);

        assertBadQuery("limit", "0");
        assertBadQuery("limit", "201");
        assertBadQuery("limit", "-1");
        assertBadQuery("limit", "abc");
        assertBadQuery("afterId", "-1");
        assertBadQuery("afterId", "abc");
        assertBadQuery("from", "not-a-timestamp");
        assertBadQuery("from", "2026-08-22T10:02:00.000Z", "to", "2026-08-22T10:02:00.000Z");
        assertBadQuery("from", "2026-08-22T10:03:00.000Z", "to", "2026-08-22T10:02:00.000Z");
        assertBadQuery("actorId", " ");
        assertBadQuery("resourceType", " ");
        assertBadQuery("resourceId", " ");
        assertBadQuery("eventType", " ");
        assertBadQuery("actorId", "a".repeat(256));
        assertBadQuery("resourceType", "a".repeat(256));
        assertBadQuery("resourceId", "a".repeat(256));
        assertBadQuery("eventType", "a".repeat(256));
    }

    @Test
    void enforcesQueryAuthorization() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501", T1);

        mockMvc.perform(get("/audit/events"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/events").with(user("writer").roles("AUDIT_WRITER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/events").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1));

        mockMvc.perform(get("/audit/events").with(user("admin").roles("AUDIT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1));
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

    private void assertQueryIds(List<Long> expectedIds, String... params) throws Exception {
        assertIds(queryJson(params), expectedIds);
    }

    private void assertIds(JsonNode response, List<Long> expectedIds) {
        List<Long> actualIds = new ArrayList<>();
        response.get("items").forEach(item -> actualIds.add(item.get("id").asLong()));
        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    private JsonNode queryJson(String... params) throws Exception {
        String response = mockMvc.perform(queryAsReader(params))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void assertBadQuery(String... params) throws Exception {
        mockMvc.perform(queryAsReader(params))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder queryAsReader(String... params) {
        MockHttpServletRequestBuilder request = get("/audit/events")
                .with(user("reader").roles("AUDIT_READER"));
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return request;
    }
}
