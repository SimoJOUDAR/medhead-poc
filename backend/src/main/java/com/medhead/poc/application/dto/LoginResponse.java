package com.medhead.poc.application.dto;

import java.time.Instant;

/**
 * JWT issuance response returned by {@code POST /api/v1/auth/login}. The token
 * is a signed HS256 JWT whose lifetime is bracketed by {@code issuedAt} and
 * {@code expiresAt}.
 */
public record LoginResponse(
        String token,
        Instant issuedAt,
        Instant expiresAt
) {
}
