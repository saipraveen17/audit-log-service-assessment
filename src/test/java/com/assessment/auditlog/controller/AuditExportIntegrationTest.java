package com.assessment.auditlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.Instant;

import com.assessment.auditlog.PostgreSqlIntegrationTestSupport;
import com.assessment.auditlog.dto.AuditExportBundle;
import com.assessment.auditlog.service.AuditExportCryptoService;
import com.assessment.auditlog.service.AuditExportVerifier;
import com.assessment.auditlog.service.AuditHashService;
import com.assessment.auditlog.service.RetentionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class AuditExportIntegrationTest extends PostgreSqlIntegrationTestSupport {

    private static final String TRUSTED_PUBLIC_KEY =
            "MCowBQYDK2VwAyEA+rxbfxrZkubbCfP874yqKBr73UPvwCccfFwChn6/oQ0=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdjustableClock testClock;

    @Autowired
    private AuditExportVerifier exportVerifier;

    @Autowired
    private AuditExportCryptoService cryptoService;

    private PublicKey trustedPublicKey;

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
        trustedPublicKey = cryptoService.publicKeyFromBase64(TRUSTED_PUBLIC_KEY);
    }

    @Test
    void exportsByActorIdWithProofHeadersAndSignature() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-202", "CLIENT_ACCOUNT", "account-501",
                "{\"field\":\"status\"}", null);
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-777",
                "{\"purpose\":\"REVIEW\"}", null);

        AuditExportBundle bundle = export("actorId", "employee-101");

        assertThat(bundle.manifest().selectorType()).isEqualTo("actorId");
        assertThat(bundle.manifest().selectorValue()).isEqualTo("employee-101");
        assertThat(bundle.manifest().selectedRecordCount()).isEqualTo(2);
        assertThat(bundle.manifest().snapshotLastId()).isEqualTo(3);
        assertThat(bundle.manifest().signatureAlgorithm()).isEqualTo("Ed25519");
        assertThat(bundle.manifest().signingKeyId()).isEqualTo("test-export-key-1");
        assertThat(bundle.selectedRecords()).hasSize(2);
        assertThat(bundle.selectedRecords()).allMatch(record -> "employee-101".equals(record.actorId()));
        assertThat(bundle.chainProofHeaders()).hasSize(3);
        assertThat(bundle.bundleDigest()).matches("[0-9a-f]{64}");
        assertThat(bundle.signature()).isNotBlank();
        assertThat(exportVerifier.verify(bundle, trustedPublicKey)).isTrue();
    }

    @Test
    void exportsByResourceIdAndIncludesArchivedMatchingEvents() throws Exception {
        long archivedEventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-202", "CLIENT_ACCOUNT", "account-501",
                "{\"field\":\"status\"}", null);
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-303", "CLIENT_ACCOUNT", "account-999",
                "{\"purpose\":\"REVIEW\"}", null);
        archiveEvent(archivedEventId);

        AuditExportBundle bundle = export("resourceId", "account-501");

        assertThat(bundle.manifest().selectorType()).isEqualTo("resourceId");
        assertThat(bundle.manifest().selectorValue()).isEqualTo("account-501");
        assertThat(bundle.selectedRecords()).hasSize(2);
        assertThat(bundle.selectedRecords()).allMatch(record -> "account-501".equals(record.resourceId()));
        assertThat(bundle.selectedRecords().getFirst().archived()).isTrue();
        assertThat(exportVerifier.verify(bundle, trustedPublicKey)).isTrue();
    }

    @Test
    void rejectsInvalidSelectors() throws Exception {
        mockMvc.perform(get("/audit/exports").with(user("reviewer").roles("COMPLIANCE_REVIEWER")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", "employee-101")
                        .param("resourceId", "account-501"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", " "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", "a".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sensitiveAndRedactedEventsExportCommittedCiphertextOnly() throws Exception {
        long eventId = createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"accountNumber\":\"1234567890\",\"purpose\":\"CUSTOMER_SUPPORT\"}",
                "[\"/accountNumber\"]");
        redact(eventId);

        String response = mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", "employee-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedRecords[0].committedPayload.accountNumber._encrypted.algorithm")
                        .value("AES-256-GCM"))
                .andExpect(jsonPath("$.selectedRecords[0].committedPayload.accountNumber._encrypted.ciphertext")
                        .isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("1234567890", "wrappedKey", "wrappingIv", "masterKey", "redactedBy");
        AuditExportBundle bundle = objectMapper.readValue(response, AuditExportBundle.class);
        assertThat(exportVerifier.verify(bundle, trustedPublicKey)).isTrue();
    }

    @Test
    void emptyMatchingResultReturnsValidSignedBundle() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);

        AuditExportBundle bundle = export("actorId", "employee-404");

        assertThat(bundle.manifest().selectedRecordCount()).isZero();
        assertThat(bundle.selectedRecords()).isEmpty();
        assertThat(bundle.chainProofHeaders()).hasSize(1);
        assertThat(exportVerifier.verify(bundle, trustedPublicKey)).isTrue();
    }

    @Test
    void independentVerifierRejectsTamperedBundleFields() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);
        AuditExportBundle bundle = export("actorId", "employee-101");

        assertThat(exportVerifier.verify(tamper(bundle, json -> ((ObjectNode) json.get("manifest"))
                .put("selectorValue", "changed")),
                trustedPublicKey)).isFalse();
        assertThat(exportVerifier.verify(tamper(bundle, json -> ((ObjectNode) json.get("selectedRecords").get(0))
                .put("actorId", "changed")), trustedPublicKey)).isFalse();
        assertThat(exportVerifier.verify(tamper(bundle, json -> ((ObjectNode) json.at("/selectedRecords/0/committedPayload"))
                .put("purpose", "changed")), trustedPublicKey)).isFalse();
        assertThat(exportVerifier.verify(tamper(bundle, json -> ((ObjectNode) json.get("chainProofHeaders").get(0))
                .put("recordHash", AuditHashService.GENESIS_HASH)), trustedPublicKey)).isFalse();
        assertThat(exportVerifier.verify(tamper(bundle, json -> json.put("bundleDigest", AuditHashService.GENESIS_HASH)),
                trustedPublicKey)).isFalse();
        assertThat(exportVerifier.verify(tamper(bundle, json -> json.put("signature", "AAAA")),
                trustedPublicKey)).isFalse();
    }

    @Test
    void refusesExportWhenSourceChainIsBroken() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);
        jdbcTemplate.update("update audit_event set actor_id = ? where id = 1", "tampered-actor");

        mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", "tampered-actor"))
                .andExpect(status().isConflict());
    }

    @Test
    void appendAfterBundleCreationDoesNotInvalidateExistingBundle() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);
        AuditExportBundle bundle = export("actorId", "employee-101");
        createEvent("CLIENT_ACCOUNT_UPDATED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"field\":\"status\"}", null);

        assertThat(bundle.manifest().snapshotLastId()).isEqualTo(1);
        assertThat(exportVerifier.verify(bundle, trustedPublicKey)).isTrue();
    }

    @Test
    void enforcesExportAuthorization() throws Exception {
        createEvent("CLIENT_ACCOUNT_VIEWED", "employee-101", "CLIENT_ACCOUNT", "account-501",
                "{\"purpose\":\"CUSTOMER_SUPPORT\"}", null);

        mockMvc.perform(get("/audit/exports").param("actorId", "employee-101"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/exports")
                        .with(user("reader").roles("AUDIT_READER"))
                        .param("actorId", "employee-101"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/exports")
                        .with(user("admin").roles("AUDIT_ADMIN"))
                        .param("actorId", "employee-101"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param("actorId", "employee-101"))
                .andExpect(status().isOk());
    }

    private AuditExportBundle export(String selectorType, String selectorValue) throws Exception {
        String response = mockMvc.perform(get("/audit/exports")
                        .with(user("reviewer").roles("COMPLIANCE_REVIEWER"))
                        .param(selectorType, selectorValue))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, AuditExportBundle.class);
    }

    private long createEvent(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payload,
            String sensitivePaths) throws Exception {
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

    private AuditExportBundle tamper(AuditExportBundle bundle, JsonTamper tamper) throws Exception {
        ObjectNode json = objectMapper.valueToTree(bundle);
        tamper.apply(json);
        return objectMapper.treeToValue(json, AuditExportBundle.class);
    }

    @FunctionalInterface
    private interface JsonTamper {

        void apply(ObjectNode json);
    }
}
