package com.medhead.poc.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed binding for the {@code app.security.*} configuration tree. Keeps the
 * JWT signing secret, token TTL and the in-memory user store out of source
 * code (E3) -- values come from {@code application.yaml} and can be overridden
 * via environment variables.
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        Jwt jwt,
        @DefaultValue List<User> users
) {

    /**
     * JWT signing configuration. The secret must be at least 256 bits when
     * used with the default HS256 algorithm; anything shorter will be rejected
     * by Nimbus at startup.
     */
    public record Jwt(
            String secret,
            @DefaultValue("60") long ttlMinutes
    ) {
    }

    /**
     * Single in-memory user. Password is expected to be an encoded value
     * recognised by {@link org.springframework.security.crypto.password.DelegatingPasswordEncoder}
     * (e.g. {@code {bcrypt}$2a$10$...}).
     */
    public record User(
            String username,
            String password
    ) {
    }
}
