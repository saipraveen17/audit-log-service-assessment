package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

class RedactionIntegrationTest extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void creationWithoutSensitivePathsStoresAndReturnsPayloadUnchanged() throws Exception {
        JsonNode response = createEvent("""
                {"purpose": "CUSTOMER_SUPPORT", "outcome": "SUCCESS"}
                """, "");

        assertThat(response.at("/payload/purpose").asText()).isEqualTo("CUSTOMER_SUPPORT");
        String storedPayload = storedPayload(response.get("id").asLong());
        assertThat(storedPayload).contains("CUSTOMER_SUPPORT");
        assertThat(storedPayload).doesNotContain("_encrypted");
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_sensitive_field_key", Long.class)).isZero();
    }

    @Test
    void sensitiveFieldsAreEncryptedAtRestAndReturnedUntilRedacted() throws Exception {
        JsonNode response = createEvent("""
                {
                  "accountNumber": "1234567890",
                  "purpose": "CUSTOMER_SUPPORT",
                  "nested": {"ssn": "111-22-3333"}
                }
                """, ",\n                  \"sensitivePaths\": [\"/accountNumber\", \"/nested/ssn\"]");
        long eventId = response.get("id").asLong();

        assertThat(response.at("/payload/accountNumber").asText()).isEqualTo("1234567890");
        assertThat(response.at("/payload/nested/ssn").asText()).isEqualTo("111-22-3333");
        assertThat(response.at("/payload/purpose").asText()).isEqualTo("CUSTOMER_SUPPORT");

        String storedPayload = storedPayload(eventId);
        assertThat(storedPayload).doesNotContain("1234567890", "111-22-3333");
        assertThat(storedPayload).contains("_encrypted", "AES-256-GCM", "ciphertext");
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_sensitive_field_key", Long.class)).isEqualTo(2);

        mockMvc.perform(get("/audit/events").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].payload.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.items[0].payload.nested.ssn").value("111-22-3333"))
                .andExpect(jsonPath("$.items[0].payload.purpose").value("CUSTOMER_SUPPORT"));

        mockMvc.perform(get("/audit/verify").with(user("verifier").roles("AUDIT_VERIFIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));
    }

    @Test
    void rejectsInvalidSensitivePaths() throws Exception {
        assertBadCreate("""
                "sensitivePaths": [""]
                """);
        assertBadCreate("""
                "sensitivePaths": ["accountNumber"]
                """);
        assertBadCreate("""
                "sensitivePaths": ["/accountNumber", "/accountNumber"]
                """);
        assertBadCreate("""
                "sensitivePaths": ["/missing"]
                """);
        assertBadCreate("""
                "sensitivePaths": ["/nested", "/nested/ssn"]
                """);
        assertBadCreate("""
                "sensitivePaths": ["/bad~2path"]
                """);
        assertBadCreate("""
                "sensitivePaths": ["/%s"]
                """.formatted("a".repeat(256)));
        assertBadCreate("""
                "sensitivePaths": [%s]
                """.formatted(quotedPaths(26)));
    }

    @Test
    void rejectsOverlongRedactionInputs() throws Exception {
        long eventId = createSensitiveEvent();

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/accountNumber"],
                                  "reason": "%s"
                                }
                                """.formatted("a".repeat(256))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/%s"],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """.formatted("a".repeat(256))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": [%s],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """.formatted(quotedPaths(26))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redactsKeyMaterialWithoutChangingCommittedPayloadHashesOrChainState() throws Exception {
        long eventId = createSensitiveEvent();
        String storedPayloadBefore = storedPayload(eventId);
        Map<String, Object> eventBefore = eventRow(eventId);
        Map<String, Object> chainBefore = chainState();

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/accountNumber"],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.redactedPaths[0]").value("/accountNumber"))
                .andExpect(jsonPath("$.alreadyRedactedPaths").isEmpty())
                .andExpect(jsonPath("$.payloadChanged").value(false));

        assertThat(storedPayload(eventId)).isEqualTo(storedPayloadBefore);
        assertThat(eventRow(eventId)).isEqualTo(eventBefore);
        assertThat(chainState()).isEqualTo(chainBefore);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_sensitive_field_key where audit_event_id = ? and wrapped_key is null and wrapping_iv is null",
                Long.class,
                eventId)).isEqualTo(1);
        Map<String, Object> redactionMetadata = jdbcTemplate.queryForMap(
                """
                        select redaction_reason, redacted_by
                        from audit_sensitive_field_key
                        where audit_event_id = ? and json_pointer = '/accountNumber'
                        """,
                eventId);
        assertThat(redactionMetadata.get("redaction_reason")).isEqualTo("DATA_PRIVACY_REQUEST");
        assertThat(redactionMetadata.get("redacted_by")).isEqualTo("admin");

        mockMvc.perform(get("/audit/events").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].payload.accountNumber.redacted").value(true))
                .andExpect(jsonPath("$.items[0].payload.purpose").value("CUSTOMER_SUPPORT"));

        mockMvc.perform(get("/audit/verify").with(user("verifier").roles("AUDIT_VERIFIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));
    }

    @Test
    void repeatedRedactionIsIdempotent() throws Exception {
        long eventId = createSensitiveEvent();
        redact(eventId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redactedPaths[0]").value("/accountNumber"))
                .andExpect(jsonPath("$.alreadyRedactedPaths").isEmpty());

        redact(eventId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redactedPaths").isEmpty())
                .andExpect(jsonPath("$.alreadyRedactedPaths[0]").value("/accountNumber"));
    }

    @Test
    void rejectsUndeclaredPathsAndUnknownEvents() throws Exception {
        long eventId = createSensitiveEvent();

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/purpose"],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/audit/events/{id}/redactions", 999)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paths": ["/accountNumber"],
                                  "reason": "DATA_PRIVACY_REQUEST"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesRedactionAuthorization() throws Exception {
        long eventId = createSensitiveEvent();
        String request = """
                {
                  "paths": ["/accountNumber"],
                  "reason": "DATA_PRIVACY_REQUEST"
                }
                """;

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("reader").roles("AUDIT_READER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }

    @Test
    void tamperedCiphertextOrWrappedKeyFailsWithoutExposingSecrets() throws Exception {
        long ciphertextEventId = createSensitiveEvent();
        jdbcTemplate.update(
                "update audit_event set payload = jsonb_set(payload, '{accountNumber,_encrypted,ciphertext}', ?::jsonb, false) where id = ?",
                "\"AAAA\"",
                ciphertextEventId);

        assertSensitivePayloadError();

        resetDatabase();
        long wrappedKeyEventId = createSensitiveEvent();
        jdbcTemplate.update(
                "update audit_sensitive_field_key set wrapped_key = decode('00', 'hex') where audit_event_id = ?",
                wrappedKeyEventId);

        assertSensitivePayloadError();
    }

    @Test
    void missingSensitiveKeyRowFailsWithoutReturningEncryptedEnvelope() throws Exception {
        long eventId = createSensitiveEvent();
        jdbcTemplate.update("delete from audit_sensitive_field_key where audit_event_id = ?", eventId);

        assertSensitivePayloadError();
    }

    @Test
    void envelopeKeyIdMismatchFailsWithoutReturningEncryptedEnvelope() throws Exception {
        long eventId = createSensitiveEvent();
        jdbcTemplate.update(
                "update audit_event set payload = jsonb_set(payload, '{accountNumber,_encrypted,keyId}', ?::jsonb, false) where id = ?",
                "\"" + UUID.randomUUID() + "\"",
                eventId);

        assertSensitivePayloadError();
    }

    @Test
    void incompleteKeyMaterialStateFailsWithoutReturningEncryptedEnvelope() throws Exception {
        long wrappedKeyNullEventId = createSensitiveEvent();
        jdbcTemplate.update(
                "update audit_sensitive_field_key set wrapped_key = null where audit_event_id = ?",
                wrappedKeyNullEventId);

        assertSensitivePayloadError();

        resetDatabase();
        long wrappingIvNullEventId = createSensitiveEvent();
        jdbcTemplate.update(
                "update audit_sensitive_field_key set wrapping_iv = null where audit_event_id = ?",
                wrappingIvNullEventId);

        assertSensitivePayloadError();
    }

    private long createSensitiveEvent() throws Exception {
        return createEvent("""
                {
                  "accountNumber": "1234567890",
                  "purpose": "CUSTOMER_SUPPORT"
                }
                """, ",\n                  \"sensitivePaths\": [\"/accountNumber\"]").get("id").asLong();
    }

    private JsonNode createEvent(String payload, String optionalFields) throws Exception {
        String body = """
                {
                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                  "actorId": "employee-101",
                  "resourceType": "CLIENT_ACCOUNT",
                  "resourceId": "account-501",
                  "payload": %s%s
                }
                """.formatted(payload, optionalFields);
        String response = mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void assertBadCreate(String optionalFields) throws Exception {
        String body = """
                {
                  "eventType": "CLIENT_ACCOUNT_VIEWED",
                  "actorId": "employee-101",
                  "resourceType": "CLIENT_ACCOUNT",
                  "resourceId": "account-501",
                  "payload": {
                    "accountNumber": "1234567890",
                    "nested": {"ssn": "111-22-3333"}
                  },
                  %s
                }
                """.formatted(optionalFields);
        mockMvc.perform(post("/audit/events")
                        .with(user("writer").roles("AUDIT_WRITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions redact(long eventId) throws Exception {
        return mockMvc.perform(post("/audit/events/{id}/redactions", eventId)
                .with(user("admin").roles("AUDIT_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "paths": ["/accountNumber"],
                          "reason": "DATA_PRIVACY_REQUEST"
                        }
                        """));
    }

    private void assertSensitivePayloadError() throws Exception {
        String response = mockMvc.perform(get("/audit/events").with(user("reader").roles("AUDIT_READER")))
                .andExpect(status().is5xxServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(response).doesNotContain(
                "1234567890",
                "wrappedKey",
                "wrappingIv",
                "ciphertext",
                "keyId",
                "\"iv\"",
                "_encrypted",
                "AES-256-GCM");
    }

    private String quotedPaths(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> "\"/field%d\"".formatted(index))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String storedPayload(long eventId) {
        return jdbcTemplate.queryForObject(
                "select payload::text from audit_event where id = ?",
                String.class,
                eventId);
    }

    private Map<String, Object> eventRow(long eventId) {
        return jdbcTemplate.queryForMap(
                """
                        select payload::text as payload, content_hash, previous_hash, record_hash, hash_version
                        from audit_event
                        where id = ?
                        """,
                eventId);
    }

    private Map<String, Object> chainState() {
        return jdbcTemplate.queryForMap(
                "select last_id, last_record_hash from audit_chain_state where name = 'GLOBAL'");
    }
}
