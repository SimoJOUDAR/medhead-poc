package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.Specialty;
import java.util.List;

/**
 * Read-side port for the NHS specialty catalogue seeded from reference data.
 */
public interface SpecialtyRepository {

    List<Specialty> findAll();
}
