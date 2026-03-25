package com.medhead.poc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.medhead.poc.application.dto.HospitalDto;
import com.medhead.poc.application.dto.HospitalDto.HospitalSpecialtySummaryDto;
import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    private static final SpecialtyGroup GENERAL_MEDICINE = new SpecialtyGroup(5L, "General medicine group");
    private static final SpecialtyGroup PATHOLOGY = new SpecialtyGroup(8L, "Pathology group");

    private static final Specialty CARDIOLOGY = new Specialty(21L, "Cardiology", GENERAL_MEDICINE);
    private static final Specialty IMMUNOLOGY = new Specialty(54L, "Immunology", PATHOLOGY);

    private static final Hospital FRED_BROOKS = new Hospital(
            1L,
            "Fred Brooks Hospital",
            new GpsCoordinates(51.5230, -0.1300),
            "123 Computing Avenue, London WC1E 6BT");
    private static final Hospital JULIA_CRUSHER = new Hospital(
            2L,
            "Julia Crusher Hospital",
            new GpsCoordinates(51.5150, -0.1400),
            "88 Regent Square, London WC1H 8HW");

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalSpecialtyRepository hospitalSpecialtyRepository;

    private HospitalService hospitalService;

    @BeforeEach
    void setUp() {
        hospitalService = new HospitalService(hospitalRepository, hospitalSpecialtyRepository);
    }

    @Test
    void listAll_shouldComposeHospitalsWithTheirSpecialtyBedSummary() {
        when(hospitalRepository.findAll()).thenReturn(List.of(FRED_BROOKS, JULIA_CRUSHER));
        when(hospitalSpecialtyRepository.findAll()).thenReturn(List.of(
                new HospitalSpecialty(100L, 1L, CARDIOLOGY, 2),
                new HospitalSpecialty(101L, 1L, IMMUNOLOGY, 3),
                new HospitalSpecialty(200L, 2L, CARDIOLOGY, 0)
        ));

        List<HospitalDto> result = hospitalService.listAll();

        assertThat(result).containsExactly(
                new HospitalDto(
                        1L,
                        "Fred Brooks Hospital",
                        51.5230,
                        -0.1300,
                        "123 Computing Avenue, London WC1E 6BT",
                        List.of(
                                new HospitalSpecialtySummaryDto(21L, "Cardiology", 2),
                                new HospitalSpecialtySummaryDto(54L, "Immunology", 3)
                        )
                ),
                new HospitalDto(
                        2L,
                        "Julia Crusher Hospital",
                        51.5150,
                        -0.1400,
                        "88 Regent Square, London WC1H 8HW",
                        List.of(
                                new HospitalSpecialtySummaryDto(21L, "Cardiology", 0)
                        )
                )
        );
    }

    @Test
    void listAll_shouldReturnHospitalWithEmptySpecialties_whenNoRowsForThatHospital() {
        when(hospitalRepository.findAll()).thenReturn(List.of(FRED_BROOKS));
        when(hospitalSpecialtyRepository.findAll()).thenReturn(List.of());

        List<HospitalDto> result = hospitalService.listAll();

        assertThat(result).singleElement()
                .satisfies(dto -> assertThat(dto.specialties()).isEmpty());
    }

    @Test
    void findById_shouldReturnHospitalWithItsSpecialties_whenFound() {
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(FRED_BROOKS));
        when(hospitalSpecialtyRepository.findAll()).thenReturn(List.of(
                new HospitalSpecialty(100L, 1L, CARDIOLOGY, 2),
                new HospitalSpecialty(101L, 1L, IMMUNOLOGY, 3),
                new HospitalSpecialty(200L, 2L, CARDIOLOGY, 0)
        ));

        HospitalDto result = hospitalService.findById(1L);

        assertThat(result).isEqualTo(new HospitalDto(
                1L,
                "Fred Brooks Hospital",
                51.5230,
                -0.1300,
                "123 Computing Avenue, London WC1E 6BT",
                List.of(
                        new HospitalSpecialtySummaryDto(21L, "Cardiology", 2),
                        new HospitalSpecialtySummaryDto(54L, "Immunology", 3)
                )
        ));
    }

    @Test
    void findById_shouldThrowHospitalNotFoundException_whenRepositoryMisses() {
        when(hospitalRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hospitalService.findById(9999L))
                .isInstanceOf(HospitalNotFoundException.class)
                .hasMessageContaining("9999");
    }
}
