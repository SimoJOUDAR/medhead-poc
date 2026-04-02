package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.model.HospitalSpecialty;
import java.util.List;

/**
 * Driven port for hospital-specialty bed availability associations. Read
 * methods feed the catalogue and recommendation use cases; the single write
 * method owns the atomic bed decrement driving §2.6's reservation step.
 */
public interface HospitalSpecialtyRepository {

    List<HospitalSpecialty> findAll();

    /**
     * Returns the rows linking the given specialty to hospitals that still
     * have at least one free bed. Backed by the partial index on
     * {@code hospital_specialties(specialty_id) WHERE available_beds > 0}.
     */
    List<HospitalSpecialty> findWithAvailableBedsForSpecialty(Long specialtyId);

    /**
     * Returns every hospital-specialty row with at least one free bed,
     * regardless of specialty. Drives the fallback flow when the requested
     * specialty has no free bed anywhere and the use case widens the search to
     * any site able to receive the patient.
     */
    List<HospitalSpecialty> findWithAnyAvailableBeds();

    /**
     * Decrements {@code available_beds} by one on the given row, under
     * Hibernate optimistic locking: the caller's {@link HospitalSpecialty}
     * carries the version it was loaded with, and the adapter rejects the
     * write if the persisted row has already advanced.
     *
     * @param hospitalSpecialty the row the caller wants to reserve a bed on,
     *                          carrying the version observed at load time
     * @return the post-reservation state (decremented beds, bumped version)
     * @throws OptimisticLockConflictException if the persisted row's version
     *         no longer matches the caller's view
     */
    HospitalSpecialty reserveBed(HospitalSpecialty hospitalSpecialty);
}
