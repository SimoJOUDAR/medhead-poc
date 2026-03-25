package com.medhead.poc.domain.exception;

/**
 * Signals that a hospital lookup targeted an id that does not exist. Thrown
 * from the application layer; translation to an HTTP status lives in the
 * inbound web adapter.
 */
public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(Long id) {
        super("Hospital not found: " + id);
    }
}
