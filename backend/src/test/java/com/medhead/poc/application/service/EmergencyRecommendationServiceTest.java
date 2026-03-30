package com.medhead.poc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.medhead.poc.domain.exception.NoSpecialtyMatchException;
import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.out.BedReservationEventPublisher;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.config.RecommendationProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final HospitalSpecialty FRED_BROOKS_CARDIO = new HospitalSpecialty(100L, 1L, CARDIOLOGY, 2, 7L);
    private static final HospitalSpecialty ALAN_TURING_CARDIO = new HospitalSpecialty(101L, 5L, CARDIOLOGY, 3, 3L);
    private static final HospitalSpecialty ANDREW_WILES_CARDIO = new HospitalSpecialty(102L, 10L, CARDIOLOGY, 1, 1L);

    private static final RouteInfo FRED_BROOKS_ROUTE = new RouteInfo(0.2, 1.0);
    private static final RouteInfo ALAN_TURING_ROUTE = new RouteInfo(260.4, 200.0);
    private static final RouteInfo ANDREW_WILES_ROUTE = new RouteInfo(280.1, 210.0);

    private static final Instant FIXED_NOW = Instant.parse("2026-04-23T09:00:00Z");

    @Mock
    private HospitalSpecialtyRepository hospitalSpecialtyRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @Mock
    private BedReservationEventPublisher eventPublisher;

    private EmergencyRecommendationService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new EmergencyRecommendationService(
                hospitalSpecialtyRepository,
                hospitalRepository,
                distanceCalculator,
                eventPublisher,
                new RecommendationProperties(3),
                fixed);
    }

    @Test
    void recommend_shouldReserveBedOnNearestHospitalAndPublishEvent() {
        stubThreeCardiologyCandidates();
        HospitalSpecialty afterReservation = new HospitalSpecialty(100L, 1L, CARDIOLOGY, 1, 8L);
        when(hospitalSpecialtyRepository.reserveBed(FRED_BROOKS_CARDIO)).thenReturn(afterReservation);

        Recommendation result = service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN));

        assertThat(result.hospital()).isEqualTo(FRED_BROOKS);
        assertThat(result.specialty()).isEqualTo(CARDIOLOGY);
        assertThat(result.availableBeds()).isEqualTo(1);
        assertThat(result.route()).isEqualTo(FRED_BROOKS_ROUTE);
        assertThat(result.bedReserved()).isTrue();
        assertThat(result.fallback()).isFalse();
        assertThat(result.timestamp()).isEqualTo(FIXED_NOW);

        ArgumentCaptor<BedReservationEvent> captor = ArgumentCaptor.forClass(BedReservationEvent.class);
        verify(eventPublisher).publish(captor.capture());
        BedReservationEvent event = captor.getValue();
        assertThat(event.hospitalId()).isEqualTo(1L);
        assertThat(event.hospitalName()).isEqualTo("Fred Brooks Hospital");
        assertThat(event.specialtyId()).isEqualTo(CARDIOLOGY_ID);
        assertThat(event.specialtyName()).isEqualTo("Cardiology");
        assertThat(event.remainingBeds()).isEqualTo(1);
        assertThat(event.timestamp()).isEqualTo(FIXED_NOW);
    }

    @Test
    void recommend_shouldBreakDistanceTieByHospitalIdAscending() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of(ALAN_TURING_CARDIO, FRED_BROOKS_CARDIO));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(FRED_BROOKS));
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(ALAN_TURING));
        when(distanceCalculator.calculate(any(), any())).thenReturn(new RouteInfo(5.0, 10.0));
        HospitalSpecialty afterReservation = new HospitalSpecialty(100L, 1L, CARDIOLOGY, 1, 8L);
        when(hospitalSpecialtyRepository.reserveBed(FRED_BROOKS_CARDIO)).thenReturn(afterReservation);

        Recommendation result = service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN));

        assertThat(result.hospital().id()).isEqualTo(1L);
        verify(hospitalSpecialtyRepository).reserveBed(FRED_BROOKS_CARDIO);
    }

    @Test
    void recommend_shouldThrowNoSpecialtyMatchException_whenCandidateSetEmpty() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN)))
                .isInstanceOf(NoSpecialtyMatchException.class)
                .hasMessageContaining(CARDIOLOGY_ID.toString());

        verifyNoInteractions(eventPublisher);
        verify(hospitalSpecialtyRepository, never()).reserveBed(any());
    }

    @Test
    void recommend_shouldRetryWithNextCandidate_whenFirstReservationLosesOptimisticLockRace() {
        stubThreeCardiologyCandidates();
        when(hospitalSpecialtyRepository.reserveBed(FRED_BROOKS_CARDIO))
                .thenThrow(new OptimisticLockConflictException(FRED_BROOKS_CARDIO.id()));
        HospitalSpecialty turingAfter = new HospitalSpecialty(101L, 5L, CARDIOLOGY, 2, 4L);
        when(hospitalSpecialtyRepository.reserveBed(ALAN_TURING_CARDIO)).thenReturn(turingAfter);

        Recommendation result = service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN));

        assertThat(result.hospital()).isEqualTo(ALAN_TURING);
        assertThat(result.availableBeds()).isEqualTo(2);
        assertThat(result.bedReserved()).isTrue();

        verify(hospitalSpecialtyRepository).reserveBed(FRED_BROOKS_CARDIO);
        verify(hospitalSpecialtyRepository).reserveBed(ALAN_TURING_CARDIO);
        verify(hospitalSpecialtyRepository, never()).reserveBed(ANDREW_WILES_CARDIO);
        ArgumentCaptor<BedReservationEvent> captor = ArgumentCaptor.forClass(BedReservationEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().hospitalId()).isEqualTo(5L);
        assertThat(captor.getValue().remainingBeds()).isEqualTo(2);
    }

    @Test
    void recommend_shouldSurfaceOptimisticLockConflict_whenEveryAttemptFails() {
        stubThreeCardiologyCandidates();
        when(hospitalSpecialtyRepository.reserveBed(any(HospitalSpecialty.class)))
                .thenThrow(new OptimisticLockConflictException(FRED_BROOKS_CARDIO.id()))
                .thenThrow(new OptimisticLockConflictException(ALAN_TURING_CARDIO.id()))
                .thenThrow(new OptimisticLockConflictException(ANDREW_WILES_CARDIO.id()));

        assertThatThrownBy(() -> service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN)))
                .isInstanceOf(OptimisticLockConflictException.class);

        verify(hospitalSpecialtyRepository).reserveBed(FRED_BROOKS_CARDIO);
        verify(hospitalSpecialtyRepository).reserveBed(ALAN_TURING_CARDIO);
        verify(hospitalSpecialtyRepository).reserveBed(ANDREW_WILES_CARDIO);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void recommend_shouldStopRetryingAfterMaxAttempts_evenIfMoreCandidatesExist() {
        Clock fixed = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new EmergencyRecommendationService(
                hospitalSpecialtyRepository,
                hospitalRepository,
                distanceCalculator,
                eventPublisher,
                new RecommendationProperties(1),
                fixed);
        stubThreeCardiologyCandidates();
        when(hospitalSpecialtyRepository.reserveBed(FRED_BROOKS_CARDIO))
                .thenThrow(new OptimisticLockConflictException(FRED_BROOKS_CARDIO.id()));

        assertThatThrownBy(() -> service.recommend(new RecommendationQuery(CARDIOLOGY_ID, ORIGIN)))
                .isInstanceOf(OptimisticLockConflictException.class);

        verify(hospitalSpecialtyRepository).reserveBed(FRED_BROOKS_CARDIO);
        verify(hospitalSpecialtyRepository, never()).reserveBed(ALAN_TURING_CARDIO);
        verify(hospitalSpecialtyRepository, never()).reserveBed(ANDREW_WILES_CARDIO);
    }

    private void stubThreeCardiologyCandidates() {
        when(hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID))
                .thenReturn(List.of(FRED_BROOKS_CARDIO, ALAN_TURING_CARDIO, ANDREW_WILES_CARDIO));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(FRED_BROOKS));
        when(hospitalRepository.findById(5L)).thenReturn(Optional.of(ALAN_TURING));
        when(hospitalRepository.findById(10L)).thenReturn(Optional.of(ANDREW_WILES));
        when(distanceCalculator.calculate(eq(ORIGIN), eq(FRED_BROOKS.coordinates())))
                .thenReturn(FRED_BROOKS_ROUTE);
        when(distanceCalculator.calculate(eq(ORIGIN), eq(ALAN_TURING.coordinates())))
                .thenReturn(ALAN_TURING_ROUTE);
        when(distanceCalculator.calculate(eq(ORIGIN), eq(ANDREW_WILES.coordinates())))
                .thenReturn(ANDREW_WILES_ROUTE);
    }
}
