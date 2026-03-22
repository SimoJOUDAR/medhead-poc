package com.medhead.poc.application.service;

import com.medhead.poc.application.dto.SpecialtyDto;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.in.ListSpecialtiesUseCase;
import com.medhead.poc.domain.port.out.SpecialtyRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Serves the seeded NHS specialty catalogue as flat DTOs. Pure read path --
 * no filtering, sorting or grouping logic beyond what the front-end needs to
 * populate a selector.
 */
@Service
public class SpecialtyService implements ListSpecialtiesUseCase {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Override
    public List<SpecialtyDto> listAll() {
        return specialtyRepository.findAll().stream()
                .map(SpecialtyService::toDto)
                .toList();
    }

    private static SpecialtyDto toDto(Specialty specialty) {
        SpecialtyGroup group = specialty.group();
        SpecialtyDto.SpecialtyGroupDto groupDto = group == null
                ? null
                : new SpecialtyDto.SpecialtyGroupDto(group.id(), group.name());
        return new SpecialtyDto(specialty.id(), specialty.name(), groupDto);
    }
}
