package com.assessment.auditlog.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class InputLimits {

    public static final int MAX_TEXT_LENGTH = 255;

    public static final int MAX_SENSITIVE_PATHS = 25;

    private InputLimits() {
    }

    public static String requireNonBlankIfPresent(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requireNonBlank(value, fieldName);
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
        }
        requireMaxLength(value, fieldName);
        return value;
    }

    public static void requireMaxLength(String value, String fieldName) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
    }
}
