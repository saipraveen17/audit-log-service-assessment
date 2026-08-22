package com.assessment.auditlog.service;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.assessment.auditlog.dto.AuditExportBundle;
import com.assessment.auditlog.dto.UnsignedAuditExportBundle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

@Service
public class AuditExportCryptoService {

    private final ObjectMapper objectMapper;

    private final JsonCanonicalizer jsonCanonicalizer;

    public AuditExportCryptoService(ObjectMapper objectMapper, JsonCanonicalizer jsonCanonicalizer) {
        this.objectMapper = objectMapper;
        this.jsonCanonicalizer = jsonCanonicalizer;
    }

    public SignedDigest sign(UnsignedAuditExportBundle unsignedBundle, PrivateKey privateKey) {
        byte[] digest = digestBytes(unsignedBundle);
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(digest);
            return new SignedDigest(
                    AuditHashService.toLowerHex(digest),
                    Base64.getEncoder().encodeToString(signature.sign()));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Audit export signing failed", exception);
        }
    }

    public byte[] digestBytes(UnsignedAuditExportBundle unsignedBundle) {
        JsonNode unsignedJson = objectMapper.valueToTree(unsignedBundle);
        String canonical = jsonCanonicalizer.canonicalize(unsignedJson);
        return AuditHashService.sha256(canonical);
    }

    public boolean verifySignature(AuditExportBundle bundle, PublicKey publicKey) {
        byte[] recalculatedDigest = digestBytes(new UnsignedAuditExportBundle(
                bundle.manifest(),
                bundle.selectedRecords(),
                bundle.chainProofHeaders()));
        if (!AuditHashService.toLowerHex(recalculatedDigest).equals(bundle.bundleDigest())) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(recalculatedDigest);
            return signature.verify(Base64.getDecoder().decode(bundle.signature()));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public PublicKey publicKeyFromBase64(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Trusted public key must be a Base64 Ed25519 X.509 key", exception);
        }
    }

    public record SignedDigest(String digestHex, String signatureBase64) {
    }
}
