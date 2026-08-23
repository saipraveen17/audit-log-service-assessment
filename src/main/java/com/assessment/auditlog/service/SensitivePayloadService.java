package com.assessment.auditlog.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.assessment.auditlog.config.RedactionProperties;
import com.assessment.auditlog.entity.AuditSensitiveFieldKey;
import com.assessment.auditlog.exception.SensitivePayloadAccessException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SensitivePayloadService {

    public static final String ENCRYPTION_MARKER = "_encrypted";

    public static final String ALGORITHM = "AES-256-GCM";

    private static final int FIELD_KEY_BYTES = 32;

    private static final int GCM_IV_BYTES = 12;

    private static final int GCM_TAG_BITS = 128;

    private final ObjectMapper objectMapper;

    private final JsonCanonicalizer jsonCanonicalizer;

    private final RedactionProperties redactionProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    public SensitivePayloadService(
            ObjectMapper objectMapper,
            JsonCanonicalizer jsonCanonicalizer,
            RedactionProperties redactionProperties) {
        this.objectMapper = objectMapper;
        this.jsonCanonicalizer = jsonCanonicalizer;
        this.redactionProperties = redactionProperties;
    }

    public PreparedPayload prepare(JsonNode payload, List<String> sensitivePaths, Instant createdAt) {
        List<ValidatedPointer> pointers = validateSensitivePaths(sensitivePaths, payload, true);
        JsonNode committedPayload = payload.deepCopy();
        List<PreparedSensitiveFieldKey> keys = new ArrayList<>();
        for (ValidatedPointer pointer : pointers) {
            JsonNode selectedValue = committedPayload.at(pointer.compiled());
            byte[] fieldKey = randomBytes(FIELD_KEY_BYTES);
            try {
                byte[] fieldIv = randomBytes(GCM_IV_BYTES);
                byte[] wrappingIv = randomBytes(GCM_IV_BYTES);
                UUID keyId = UUID.randomUUID();
                byte[] ciphertext = encrypt(fieldKey, fieldIv, jsonCanonicalizer.canonicalize(selectedValue).getBytes(StandardCharsets.UTF_8));
                byte[] wrappedKey = wrapFieldKey(fieldKey, wrappingIv);
                replaceAtPath(committedPayload, pointer.tokens(), encryptedEnvelope(keyId, fieldIv, ciphertext));
                keys.add(new PreparedSensitiveFieldKey(keyId, pointer.path(), wrappedKey, wrappingIv, createdAt));
            } finally {
                Arrays.fill(fieldKey, (byte) 0);
            }
        }
        return new PreparedPayload(committedPayload, keys);
    }

    public List<String> validateRedactionPaths(List<String> paths) {
        return validateSensitivePaths(paths, null, false).stream()
                .map(ValidatedPointer::path)
                .toList();
    }

    public JsonNode logicalPayload(JsonNode committedPayload, List<AuditSensitiveFieldKey> keys) {
        JsonNode logicalPayload = committedPayload.deepCopy();
        List<AuditSensitiveFieldKey> orderedKeys = keys.stream()
                .sorted(Comparator.comparing(AuditSensitiveFieldKey::getJsonPointer))
                .toList();
        for (AuditSensitiveFieldKey key : orderedKeys) {
            ValidatedPointer pointer = validateSinglePath(key.getJsonPointer());
            JsonNode envelope = logicalPayload.at(pointer.compiled());
            if (envelope.isMissingNode() || !isEncryptedEnvelope(envelope)) {
                throw new SensitivePayloadAccessException();
            }
            validateEnvelopeKeyId(envelope, key);
            JsonNode replacement;
            if (isActive(key)) {
                replacement = decryptEnvelope(envelope, key);
            } else if (isLegitimatelyRedacted(key)) {
                replacement = redactedMarker();
            } else {
                throw new SensitivePayloadAccessException();
            }
            replaceAtPath(logicalPayload, pointer.tokens(), replacement);
        }
        if (containsEncryptedEnvelope(logicalPayload)) {
            throw new SensitivePayloadAccessException();
        }
        return logicalPayload;
    }

    public Map<Long, List<AuditSensitiveFieldKey>> groupByEventId(List<AuditSensitiveFieldKey> keys) {
        return keys.stream().collect(Collectors.groupingBy(AuditSensitiveFieldKey::getAuditEventId));
    }

    private List<ValidatedPointer> validateSensitivePaths(List<String> paths, JsonNode payload, boolean requireExisting) {
        if (paths == null) {
            return List.of();
        }
        if (paths.isEmpty()) {
            return List.of();
        }
        if (paths.size() > InputLimits.MAX_SENSITIVE_PATHS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "JSON Pointer paths must contain at most " + InputLimits.MAX_SENSITIVE_PATHS + " paths");
        }
        List<ValidatedPointer> pointers = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String path : paths) {
            ValidatedPointer pointer = validateSinglePath(path);
            if (!seen.add(pointer.path())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sensitivePaths must not contain duplicates");
            }
            if (requireExisting && payload.at(pointer.compiled()).isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sensitivePaths must exist in payload");
            }
            pointers.add(pointer);
        }
        rejectOverlaps(pointers);
        return pointers;
    }

    private ValidatedPointer validateSinglePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Pointer path must not be blank");
        }
        InputLimits.requireMaxLength(path, "JSON Pointer path");
        if (!path.startsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Pointer path must start with /");
        }
        List<String> tokens = parseTokens(path);
        if (tokens.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Root JSON Pointer path is not allowed");
        }
        try {
            return new ValidatedPointer(path, JsonPointer.compile(path), tokens);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Pointer path", exception);
        }
    }

    private List<String> parseTokens(String path) {
        String[] rawTokens = path.substring(1).split("/", -1);
        List<String> tokens = new ArrayList<>(rawTokens.length);
        for (String rawToken : rawTokens) {
            StringBuilder token = new StringBuilder(rawToken.length());
            for (int i = 0; i < rawToken.length(); i++) {
                char current = rawToken.charAt(i);
                if (current == '~') {
                    if (i + 1 >= rawToken.length()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Pointer escape");
                    }
                    char escaped = rawToken.charAt(++i);
                    if (escaped == '0') {
                        token.append('~');
                    } else if (escaped == '1') {
                        token.append('/');
                    } else {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Pointer escape");
                    }
                } else {
                    token.append(current);
                }
            }
            tokens.add(token.toString());
        }
        return tokens;
    }

    private void rejectOverlaps(List<ValidatedPointer> pointers) {
        for (int i = 0; i < pointers.size(); i++) {
            for (int j = i + 1; j < pointers.size(); j++) {
                if (isPrefix(pointers.get(i).tokens(), pointers.get(j).tokens())
                        || isPrefix(pointers.get(j).tokens(), pointers.get(i).tokens())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Pointer paths must not overlap");
                }
            }
        }
    }

    private boolean isPrefix(List<String> possiblePrefix, List<String> value) {
        if (possiblePrefix.size() >= value.size()) {
            return false;
        }
        for (int i = 0; i < possiblePrefix.size(); i++) {
            if (!possiblePrefix.get(i).equals(value.get(i))) {
                return false;
            }
        }
        return true;
    }

    private ObjectNode encryptedEnvelope(UUID keyId, byte[] iv, byte[] ciphertext) {
        ObjectNode encrypted = objectMapper.createObjectNode();
        encrypted.put("keyId", keyId.toString());
        encrypted.put("algorithm", ALGORITHM);
        encrypted.put("iv", Base64.getEncoder().encodeToString(iv));
        encrypted.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.set(ENCRYPTION_MARKER, encrypted);
        return envelope;
    }

    private ObjectNode redactedMarker() {
        ObjectNode marker = objectMapper.createObjectNode();
        marker.put("redacted", true);
        return marker;
    }

    private JsonNode decryptEnvelope(JsonNode envelope, AuditSensitiveFieldKey key) {
        try {
            JsonNode encrypted = envelope.get(ENCRYPTION_MARKER);
            if (!ALGORITHM.equals(encrypted.path("algorithm").asText())) {
                throw new SensitivePayloadAccessException();
            }
            byte[] fieldKey = unwrapFieldKey(key);
            try {
                byte[] iv = Base64.getDecoder().decode(encrypted.path("iv").asText());
                byte[] ciphertext = Base64.getDecoder().decode(encrypted.path("ciphertext").asText());
                byte[] plaintext = decrypt(fieldKey, iv, ciphertext);
                return objectMapper.readTree(new String(plaintext, StandardCharsets.UTF_8));
            } finally {
                Arrays.fill(fieldKey, (byte) 0);
            }
        } catch (GeneralSecurityException | IllegalArgumentException | JsonProcessingException exception) {
            throw new SensitivePayloadAccessException(exception);
        }
    }

    private void validateEnvelopeKeyId(JsonNode envelope, AuditSensitiveFieldKey key) {
        try {
            UUID envelopeKeyId = UUID.fromString(envelope.get(ENCRYPTION_MARKER).path("keyId").asText());
            if (!envelopeKeyId.equals(key.getKeyId())) {
                throw new SensitivePayloadAccessException();
            }
        } catch (IllegalArgumentException exception) {
            throw new SensitivePayloadAccessException(exception);
        }
    }

    private boolean isActive(AuditSensitiveFieldKey key) {
        return key.getWrappedKey() != null && key.getWrappingIv() != null;
    }

    private boolean isLegitimatelyRedacted(AuditSensitiveFieldKey key) {
        return key.getWrappedKey() == null
                && key.getWrappingIv() == null
                && key.getRedactedAt() != null
                && key.getRedactionReason() != null
                && !key.getRedactionReason().isBlank()
                && key.getRedactedBy() != null
                && !key.getRedactedBy().isBlank();
    }

    private byte[] wrapFieldKey(byte[] fieldKey, byte[] wrappingIv) {
        byte[] masterKey = redactionProperties.masterKey();
        try {
            return encrypt(masterKey, wrappingIv, fieldKey);
        } finally {
            Arrays.fill(masterKey, (byte) 0);
        }
    }

    private byte[] unwrapFieldKey(AuditSensitiveFieldKey key) throws GeneralSecurityException {
        byte[] masterKey = redactionProperties.masterKey();
        try {
            return decrypt(masterKey, key.getWrappingIv(), key.getWrappedKey());
        } finally {
            Arrays.fill(masterKey, (byte) 0);
        }
    }

    private boolean isEncryptedEnvelope(JsonNode node) {
        JsonNode encrypted = node.get(ENCRYPTION_MARKER);
        return node.isObject()
                && encrypted != null
                && encrypted.isObject()
                && encrypted.hasNonNull("keyId")
                && encrypted.hasNonNull("algorithm")
                && encrypted.hasNonNull("iv")
                && encrypted.hasNonNull("ciphertext");
    }

    private boolean containsEncryptedEnvelope(JsonNode node) {
        if (isEncryptedEnvelope(node)) {
            return true;
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                if (containsEncryptedEnvelope(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void replaceAtPath(JsonNode root, List<String> tokens, JsonNode replacement) {
        JsonNode parent = root;
        for (int i = 0; i < tokens.size() - 1; i++) {
            parent = child(parent, tokens.get(i));
        }
        String lastToken = tokens.getLast();
        if (parent instanceof ObjectNode objectNode) {
            objectNode.set(lastToken, replacement);
            return;
        }
        if (parent instanceof ArrayNode arrayNode) {
            try {
                arrayNode.set(Integer.parseInt(lastToken), replacement);
                return;
            } catch (NumberFormatException | IndexOutOfBoundsException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Pointer path could not be replaced", exception);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Pointer path could not be replaced");
    }

    private JsonNode child(JsonNode parent, String token) {
        if (parent.isArray()) {
            try {
                return parent.path(Integer.parseInt(token));
            } catch (NumberFormatException exception) {
                return parent.path(token);
            }
        }
        return parent.path(token);
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) {
        try {
            return cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(plaintext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Encryption failed", exception);
        }
    }

    private byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) throws GeneralSecurityException {
        return cipher(Cipher.DECRYPT_MODE, key, iv).doFinal(ciphertext);
    }

    private Cipher cipher(int mode, byte[] key, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher;
    }

    private record ValidatedPointer(String path, JsonPointer compiled, List<String> tokens) {
    }

    public record PreparedPayload(JsonNode committedPayload, List<PreparedSensitiveFieldKey> keys) {

        public PreparedPayload {
            committedPayload = committedPayload.deepCopy();
            keys = List.copyOf(keys);
        }

        @Override
        public JsonNode committedPayload() {
            return committedPayload.deepCopy();
        }
    }

    public record PreparedSensitiveFieldKey(
            UUID keyId,
            String jsonPointer,
            byte[] wrappedKey,
            byte[] wrappingIv,
            Instant createdAt) {

        public PreparedSensitiveFieldKey {
            wrappedKey = wrappedKey.clone();
            wrappingIv = wrappingIv.clone();
        }

        public AuditSensitiveFieldKey toEntity(long auditEventId) {
            return new AuditSensitiveFieldKey(keyId, auditEventId, jsonPointer, wrappedKey, wrappingIv, createdAt);
        }

        @Override
        public byte[] wrappedKey() {
            return wrappedKey.clone();
        }

        @Override
        public byte[] wrappingIv() {
            return wrappingIv.clone();
        }
    }
}
