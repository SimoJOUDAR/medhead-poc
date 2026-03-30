package com.medhead.poc.domain.model;

import java.time.Instant;

/**
 * Business event emitted after a bed is reserved for an emergency
 * recommendation. Carries only the ids, names and counts required for
 * downstream auxiliary systems (B6 event-driven extension) -- no patient data
 * is ever part of the payload (E2).
 */
public record BedReservationEvent(
        Long hospitalId,
        String hospitalName,
        Long specialtyId,
        String specialtyName,
        int remainingBeds,
        Instant timestamp
) {
}
