package com.medhead.poc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.medhead.poc.application.dto.SpecialtyDto;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.out.SpecialtyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private SpecialtyService specialtyService;

    @BeforeEach
    void setUp() {
        specialtyService = new SpecialtyService(specialtyRepository);
    }

    @Test
    void listAll_shouldMapDomainSpecialtiesToDtos_preservingGroup() {
        SpecialtyGroup cardiothoracic = new SpecialtyGroup(1L, "Cardiothoracic");
        SpecialtyGroup immunology = new SpecialtyGroup(2L, "Immunology");
        when(specialtyRepository.findAll()).thenReturn(List.of(
                new Specialty(10L, "Cardiology", cardiothoracic),
                new Specialty(20L, "Immunology", immunology)
        ));

        List<SpecialtyDto> result = specialtyService.listAll();

        assertThat(result).containsExactly(
                new SpecialtyDto(10L, "Cardiology", new SpecialtyDto.SpecialtyGroupDto(1L, "Cardiothoracic")),
                new SpecialtyDto(20L, "Immunology", new SpecialtyDto.SpecialtyGroupDto(2L, "Immunology"))
        );
    }

    @Test
    void listAll_shouldReturnEmptyList_whenRepositoryIsEmpty() {
        when(specialtyRepository.findAll()).thenReturn(List.of());

        List<SpecialtyDto> result = specialtyService.listAll();

        assertThat(result).isEmpty();
    }

    @Test
    void listAll_shouldTolerateNullGroup() {
        when(specialtyRepository.findAll()).thenReturn(List.of(
                new Specialty(99L, "Orphan", null)
        ));

        List<SpecialtyDto> result = specialtyService.listAll();

        assertThat(result).containsExactly(new SpecialtyDto(99L, "Orphan", null));
    }
}
