package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.Hospital;
import java.util.List;
import java.util.Optional;

/**
 * Read-side port for hospitals. Subsequent sessions extend this contract with
 * the write operations (e.g. bed reservation) they actually require.
 */
public interface HospitalRepository {

    List<Hospital> findAll();

    Optional<Hospital> findById(Long id);
}
