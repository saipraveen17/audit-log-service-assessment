package com.assessment.auditlog.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Pattern USER_ROLE_PATTERN =
            Pattern.compile("AUDIT_SECURITY_USERS_(\\d+)_ROLES_(\\d+)");

    private static final Map<String, String> PROPERTY_MAPPINGS = Map.of(
            "POSTGRES_JDBC_URL", "spring.datasource.url",
            "POSTGRES_USER", "spring.datasource.username",
            "POSTGRES_PASSWORD", "spring.datasource.password",
            "AUDIT_RETENTION_DAYS", "audit.retention-days",
            "AUDIT_REDACTION_MASTER_KEY_BASE64", "audit.redaction.master-key-base64",
            "AUDIT_EXPORT_SIGNING_KEY_ID", "audit.export.signing-key-id",
            "AUDIT_EXPORT_PRIVATE_KEY_BASE64", "audit.export.private-key-base64");

    private static final String PROPERTY_SOURCE_NAME = "localDotenv";

    private final Path dotenv;

    public DotenvEnvironmentPostProcessor() {
        this(Path.of(".env"));
    }

    DotenvEnvironmentPostProcessor(Path dotenv) {
        this.dotenv = dotenv;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Files.isRegularFile(dotenv) || !Files.isReadable(dotenv)) {
            return;
        }

        Map<String, Object> properties;
        try {
            properties = parse(Files.readAllLines(dotenv));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read local .env file", exception);
        }
        if (properties.isEmpty()) {
            return;
        }

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    static Map<String, Object> parse(Iterable<String> lines) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String line : lines) {
            ParsedLine parsedLine = parseLine(line);
            if (parsedLine == null) {
                continue;
            }
            properties.put(parsedLine.key(), parsedLine.value());
            String mappedKey = mappedPropertyKey(parsedLine.key());
            if (mappedKey != null) {
                properties.put(mappedKey, parsedLine.value());
            }
        }
        return properties;
    }

    private static ParsedLine parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex <= 0) {
            return null;
        }
        String key = trimmed.substring(0, equalsIndex).trim();
        String value = stripOptionalQuotes(trimmed.substring(equalsIndex + 1).trim());
        if (key.isEmpty()) {
            return null;
        }
        return new ParsedLine(key, value);
    }

    private static String stripOptionalQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String mappedPropertyKey(String key) {
        String directMapping = PROPERTY_MAPPINGS.get(key);
        if (directMapping != null) {
            return directMapping;
        }
        Matcher roleMatcher = USER_ROLE_PATTERN.matcher(key);
        if (roleMatcher.matches()) {
            return "audit.security.users[%s].roles[%s]".formatted(
                    roleMatcher.group(1),
                    roleMatcher.group(2));
        }
        if (key.startsWith("AUDIT_SECURITY_USERS_")) {
            return key.substring("AUDIT_SECURITY_USERS_".length())
                    .toLowerCase()
                    .replaceFirst("_(username|password)$", ".$1")
                    .replaceFirst("^(\\d+)\\.", "audit.security.users[$1].");
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private record ParsedLine(String key, String value) {
    }
}
