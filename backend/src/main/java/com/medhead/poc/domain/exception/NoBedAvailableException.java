package com.medhead.poc.domain.exception;

/**
 * Signals that no hospital anywhere has an available bed -- neither for the
 * requested specialty nor for the fallback set. Thrown by the recommendation
 * service after both candidate queries come back empty; mapped to HTTP 404 by
 * the global exception handler introduced in a later PR.
 */
public class NoBedAvailableException extends RuntimeException {

    private final Long requestedSpecialtyId;

    public NoBedAvailableException(Long requestedSpecialtyId) {
        super("No hospital has an available bed for the requested specialty "
                + "or any fallback: " + requestedSpecialtyId);
        this.requestedSpecialtyId = requestedSpecialtyId;
    }

    public Long requestedSpecialtyId() {
        return requestedSpecialtyId;
    }
}
