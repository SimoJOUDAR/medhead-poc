package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.HospitalSpecialty;
import java.util.List;

/**
 * Read-side port for hospital-specialty bed availability associations.
 */
public interface HospitalSpecialtyRepository {

    List<HospitalSpecialty> findAll();

    /**
     * Returns the rows linking the given specialty to hospitals that still
     * have at least one free bed. Backed by the partial index on
     * {@code hospital_specialties(specialty_id) WHERE available_beds > 0}.
     */
    List<HospitalSpecialty> findWithAvailableBedsForSpecialty(Long specialtyId);
}
