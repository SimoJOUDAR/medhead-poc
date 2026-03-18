package com.medhead.poc.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials payload for {@code POST /api/v1/auth/login}. Both fields are
 * required; validation is enforced at the controller boundary (E4).
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
