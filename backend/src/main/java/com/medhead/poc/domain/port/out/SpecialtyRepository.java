package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.Specialty;
import java.util.List;
import java.util.Optional;

/**
 * Read-side port for the NHS specialty catalogue seeded from reference data.
 */
public interface SpecialtyRepository {

    List<Specialty> findAll();

    /**
     * Looks up a single specialty by its catalogue identifier. Used by the
     * recommendation use case when the fallback path must describe the
     * originally-requested specialty in the response even though no bed for it
     * was available.
     */
    Optional<Specialty> findById(Long id);
}
