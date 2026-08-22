package com.assessment.auditlog.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.security")
public class AuditSecurityProperties {

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
}
