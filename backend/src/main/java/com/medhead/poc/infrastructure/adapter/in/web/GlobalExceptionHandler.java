package com.medhead.poc.infrastructure.adapter.in.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central error-to-HTTP translation (E1). Controllers stay free of inline
 * {@code try/catch}; expected failure modes are mapped here to structured
 * error responses with a stable shape. S5 will extend this handler with
 * domain exceptions as they are introduced.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "invalid_credentials",
                "message", "Invalid username or password"
        ));
    }
}
