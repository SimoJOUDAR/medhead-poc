package com.medhead.poc.domain.model;

/**
 * Bed-availability link between a hospital and a specialty. The {@code version}
 * field propagates the row's current optimistic-locking stamp from the JPA
 * entity to the domain layer, so the reservation port can re-attach it on save
 * and trip Hibernate's version check when a concurrent writer has already
 * decremented the same row.
 */
public record HospitalSpecialty(Long id, Long hospitalId, Specialty specialty, int availableBeds, long version) {
}
