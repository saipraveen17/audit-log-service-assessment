package com.assessment.auditlog.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/audit/events").hasAnyRole("AUDIT_WRITER", "AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/audit/events").hasAnyRole("AUDIT_READER", "AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/audit/verify").hasAnyRole("AUDIT_VERIFIER", "AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/audit/retention/run").hasRole("AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/audit/events/*/redactions").hasRole("AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/audit/exports")
                        .hasAnyRole("COMPLIANCE_REVIEWER", "AUDIT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/audit/compliance/client-account-access")
                        .hasAnyRole("COMPLIANCE_REVIEWER", "AUDIT_ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(AuditSecurityProperties properties) {
        List<UserDetails> users = properties.getUsers().stream()
                .map(configuredUser -> User.withUsername(configuredUser.getUsername())
                        .password(configuredUser.getPassword())
                        .roles(configuredUser.getRoles().toArray(String[]::new))
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
