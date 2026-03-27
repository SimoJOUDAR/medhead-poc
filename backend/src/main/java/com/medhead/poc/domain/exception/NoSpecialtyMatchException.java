package com.medhead.poc.domain.exception;

/**
 * Signals that no hospital currently has an available bed for the requested
 * specialty. Thrown by the recommendation service when the specialty-specific
 * candidate set is empty. PR-S5-07 will catch this to trigger the fallback
 * flow; in this PR it surfaces as the default HTTP 500.
 */
public class NoSpecialtyMatchException extends RuntimeException {

    private final Long specialtyId;

    public NoSpecialtyMatchException(Long specialtyId) {
        super("No hospital has an available bed for specialty: " + specialtyId);
        this.specialtyId = specialtyId;
    }

    public Long specialtyId() {
        return specialtyId;
    }
}
