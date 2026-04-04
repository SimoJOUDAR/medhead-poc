package com.medhead.poc.domain.exception;

/**
 * Signals that a specialty lookup targeted an id that does not exist. Thrown
 * from the application layer; translation to an HTTP status lives in the
 * inbound web adapter.
 */
public class SpecialtyNotFoundException extends RuntimeException {

    public SpecialtyNotFoundException(Long id) {
        super("Specialty not found: " + id);
    }
}
