package com.assessment.auditlog.exception;

public class SensitivePayloadAccessException extends RuntimeException {

    public SensitivePayloadAccessException() {
        super("Sensitive payload could not be processed.");
    }

    public SensitivePayloadAccessException(Throwable cause) {
        super("Sensitive payload could not be processed.", cause);
    }
}
