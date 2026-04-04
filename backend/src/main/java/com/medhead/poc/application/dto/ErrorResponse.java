package com.medhead.poc.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error payload returned by {@link
 * com.medhead.poc.infrastructure.adapter.in.web.GlobalExceptionHandler} across
 * every failure mode. Stable shape so consumers can parse the same fields for
 * every 4xx/5xx response regardless of which exception mapped to them.
 *
 * <p>{@code code} is a stable machine-readable identifier
 * ({@code NO_BEDS_AVAILABLE}, {@code HOSPITAL_NOT_FOUND}, ...). {@code details}
 * carries per-field messages for validation failures and is {@code null} when
 * no field-level information applies.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldError> details
) {

    public record FieldError(String field, String message) {
    }
}
