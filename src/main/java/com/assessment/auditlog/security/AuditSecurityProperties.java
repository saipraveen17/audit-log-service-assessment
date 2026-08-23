package com.assessment.auditlog.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.assessment.auditlog.service.InputLimits;
import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "audit.security")
public class AuditSecurityProperties {

    public static final Set<String> SUPPORTED_ROLES = Set.of(
            "AUDIT_WRITER",
            "AUDIT_READER",
            "AUDIT_VERIFIER",
            "AUDIT_ADMIN",
            "COMPLIANCE_REVIEWER");

    private final List<User> users = new ArrayList<>();

    public List<User> getUsers() {
        return users;
    }

    public static class User {

        private String username;

        private String password;

        private List<String> roles = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? new ArrayList<>() : roles;
        }
    }

    @AssertTrue(message = "audit.security.users entries must have unique nonblank usernames, nonblank passwords, and supported roles")
    public boolean isValidUsers() {
        Set<String> seenUsernames = new HashSet<>();
        for (User user : users) {
            if (user == null
                    || isBlankOrTooLong(user.getUsername())
                    || isBlankOrTooLong(user.getPassword())
                    || !seenUsernames.add(user.getUsername())
                    || user.getRoles().isEmpty()) {
                return false;
            }
            for (String role : user.getRoles()) {
                if (role == null || role.isBlank() || !SUPPORTED_ROLES.contains(role)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBlankOrTooLong(String value) {
        return value == null || value.isBlank() || value.length() > InputLimits.MAX_TEXT_LENGTH;
    }
}
