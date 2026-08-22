package com.assessment.auditlog.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;

@Service
public class AuditHashService {

    public static final int HASH_VERSION = 1;

    public static final String GENESIS_HASH = sha256Hex("AUDIT_LOG_GENESIS_V1");

    private final JsonCanonicalizer jsonCanonicalizer;

    public AuditHashService(JsonCanonicalizer jsonCanonicalizer) {
        this.jsonCanonicalizer = jsonCanonicalizer;
    }

    public String contentHash(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            JsonNode committedPayload,
            Instant timestamp) {
        ObjectNode hashInput = JsonNodeFactory.instance.objectNode();
        hashInput.put("eventType", eventType);
        hashInput.put("actorId", actorId);
        hashInput.put("resourceType", resourceType);
        hashInput.put("resourceId", resourceId);
        hashInput.set("committedPayload", committedPayload.deepCopy());
        hashInput.put("timestamp", TimeFormats.formatUtcMillis(timestamp));
        return sha256Hex(jsonCanonicalizer.canonicalize(hashInput));
    }

    public String recordHash(int hashVersion, long id, String contentHash, String previousHash) {
        String canonical = "hashVersion=" + hashVersion
                + "\nid=" + id
                + "\ncontentHash=" + contentHash
                + "\npreviousHash=" + previousHash;
        return sha256Hex(canonical);
    }

    public static String sha256Hex(String value) {
        return toLowerHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static String toLowerHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
