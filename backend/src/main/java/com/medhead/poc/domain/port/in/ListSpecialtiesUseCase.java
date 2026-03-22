package com.medhead.poc.domain.port.in;

import com.medhead.poc.application.dto.SpecialtyDto;
import java.util.List;

/**
 * Driving port exposing the seeded NHS specialty catalogue to clients selecting
 * the kind of care a patient requires.
 */
public interface ListSpecialtiesUseCase {

    List<SpecialtyDto> listAll();
}
