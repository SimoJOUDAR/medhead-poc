package com.medhead.poc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.medhead.poc.domain.exception.NoSpecialtyMatchException;
import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyRecommendationServiceTest {

    private static final Long CARDIOLOGY_ID = 21L;
    private static final SpecialtyGroup GENERAL_MEDICINE = new SpecialtyGroup(5L, "General medicine group");
    private static final Specialty CARDIOLOGY = new Specialty(CARDIOLOGY_ID, "Cardiology", GENERAL_MEDICINE);

    private static final GpsCoordinates ORIGIN = new GpsCoordinates(51.5230, -0.1310);

    private static final Hospital FRED_BROOKS = new Hospital(
            1L, "Fred Brooks Hospital", new GpsCoordinates(51.5230, -0.1300), "123 Computing Avenue");
    private static final Hospital ALAN_TURING = new Hospital(
            5L, "Alan Turing Hospital", new GpsCoordinates(53.4700, -2.2300), "17 Bletchley Street");
    private static final Hospital ANDREW_WILES = new Hospital(
            10L, "Andrew Wiles Hospital", new GpsCoordinates(53.8008, -1.5491), "7 Fermat Court");

    private static final Instant FIXED_NOW = Instant.parse("2026-04-23T09:00:00Z");

    @Mock
    private HospitalSpecialtyRepository hospitalSpecialtyRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    private EmergencyRecommendationService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new EmergencyRecommendationService(
                hospitalSpecialtyRepository, hospitalRepository, distanceCalculator, fixed);
    }

    @Test
    void recommend_shouldSelectNearestHospitalWithAvailableBedForRequestedSpecialty() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of(
                        new HospitalSpecialty(100L, 1L, CARDIOLOGY, 2),
                        new HospitalSpecialty(101L, 5L, CARDIOLOGY, 3),
                        new HospitalSpecialty(102L, 10L, CARDIOLOGY, 1)
                ));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(FRED_BROOKS));
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(ALAN_TURING));
        when(hospitalRepository.findById(10L)).thenReturn(Optional.of(ANDREW_WILES));
        when(distanceCalculator.calculate(eq(ORIGIN), eq(FRED_BROOKS.coordinates())))
                .thenReturn(new RouteInfo(0.2, 1.0));
        when(distanceCalculator.calculate(eq(ORIGIN), eq(ALAN_TURING.coordinates())))
                .thenReturn(new RouteInfo(260.4, 200.0));
        when(distanceCalculator.calculate(eq(ORIGIN), eq(ANDREW_WILES.coordinates())))
                .thenReturn(new RouteInfo(280.1, 210.0));

        Recommendation result = service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN));

        assertThat(result.hospital()).isEqualTo(FRED_BROOKS);
        assertThat(result.specialty()).isEqualTo(CARDIOLOGY);
        assertThat(result.availableBeds()).isEqualTo(2);
        assertThat(result.route()).isEqualTo(new RouteInfo(0.2, 1.0));
        assertThat(result.bedReserved()).isFalse();
        assertThat(result.fallback()).isFalse();
        assertThat(result.timestamp()).isEqualTo(FIXED_NOW);
    }

    @Test
    void recommend_shouldBreakDistanceTieByHospitalIdAscending() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of(
                        new HospitalSpecialty(101L, 5L, CARDIOLOGY, 3),
                        new HospitalSpecialty(100L, 1L, CARDIOLOGY, 2)
                ));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(FRED_BROOKS));
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(ALAN_TURING));
        when(distanceCalculator.calculate(any(), any())).thenReturn(new RouteInfo(5.0, 10.0));

        Recommendation result = service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN));

        assertThat(result.hospital().id()).isEqualTo(1L);
    }

    @Test
    void recommend_shouldThrowNoSpecialtyMatchException_whenCandidateSetEmpty() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN)))
                .isInstanceOf(NoSpecialtyMatchException.class)
                .hasMessageContaining(CARDIOLOGY_ID.toString());
    }
}
