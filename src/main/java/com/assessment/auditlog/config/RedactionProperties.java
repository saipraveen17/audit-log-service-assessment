package com.assessment.auditlog.config;

import java.util.Base64;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "audit.redaction")
public class RedactionProperties {

    private static final int MASTER_KEY_BYTES = 32;

    @NotBlank
    private String masterKeyBase64;

    public String getMasterKeyBase64() {
        return masterKeyBase64;
    }

    public void setMasterKeyBase64(String masterKeyBase64) {
        this.masterKeyBase64 = masterKeyBase64;
    }

    public byte[] masterKey() {
        return Base64.getDecoder().decode(masterKeyBase64);
    }

    @AssertTrue(message = "audit.redaction.master-key-base64 must decode to exactly 32 bytes")
    public boolean isValidMasterKey() {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(masterKeyBase64).length == MASTER_KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
