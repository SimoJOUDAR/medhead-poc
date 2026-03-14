package com.medhead.poc.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security posture for the walking skeleton.
 *
 * <p>Stateless REST API: CSRF, HTTP Basic and form login are all disabled.
 * Only the anonymous endpoints needed at this stage are permitted; every
 * other request is rejected. The permit list is deliberately narrow so each
 * subsequent PR makes an explicit decision when it exposes a new surface.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/ping").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    /**
     * Empty in-memory user store. Its sole purpose is to suppress Spring Boot's
     * {@code UserDetailsServiceAutoConfiguration}, which otherwise generates a
     * random password at startup and logs a misleading security warning. This
     * filter chain has HTTP Basic disabled so no user store is actually needed;
     * S5 will replace this with JWT-based authentication.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
