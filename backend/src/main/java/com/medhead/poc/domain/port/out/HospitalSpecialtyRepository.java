package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.HospitalSpecialty;
import java.util.List;

/**
 * Read-side port for hospital-specialty bed availability associations.
 */
public interface HospitalSpecialtyRepository {

    List<HospitalSpecialty> findAll();
}
