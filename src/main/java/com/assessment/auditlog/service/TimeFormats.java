package com.assessment.auditlog.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class TimeFormats {

    private static final DateTimeFormatter UTC_MILLIS_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private TimeFormats() {
    }

    public static Instant truncateToMillis(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }

    public static String formatUtcMillis(Instant instant) {
        return UTC_MILLIS_FORMATTER.format(truncateToMillis(instant));
    }
}
