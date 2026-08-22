package com.assessment.auditlog.config;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "audit.export")
public class ExportProperties {

    @NotBlank
    private String signingKeyId;

    @NotBlank
    private String privateKeyBase64;

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public String getPrivateKeyBase64() {
        return privateKeyBase64;
    }

    public void setPrivateKeyBase64(String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    public PrivateKey privateKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new IllegalStateException("audit.export.private-key-base64 must contain a valid Ed25519 PKCS#8 key", exception);
        }
    }

    @AssertTrue(message = "audit.export.private-key-base64 must contain a valid Ed25519 PKCS#8 key")
    public boolean isValidPrivateKey() {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            return false;
        }
        try {
            privateKey();
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
