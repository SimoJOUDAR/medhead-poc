package com.medhead.poc.application.service;

import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.exception.NoBedAvailableException;
import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.exception.SpecialtyNotFoundException;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.port.in.RecommendHospitalUseCase;
import com.medhead.poc.domain.port.out.BedReservationEventPublisher;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.domain.port.out.SpecialtyRepository;
import com.medhead.poc.infrastructure.config.RecommendationProperties;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Core hospital-recommendation use case. Selects the nearest hospital with a
 * free bed in the requested specialty, reserves that bed under optimistic
 * locking, and announces the reservation as a {@link BedReservationEvent}.
 * When the reservation loses the optimistic-lock race against a concurrent
 * writer, the service falls through to the next-nearest candidate up to
 * {@link RecommendationProperties#maxReservationAttempts()} attempts before
 * surrendering.
 *
 * <p>When no hospital has a bed in the requested specialty, the service widens
 * the search to any hospital with at least one free bed (fallback flow,
 * mitigating risk #3 in the risk register) and flags the resulting
 * {@link Recommendation} so the caller can surface the specialty swap. If that
 * fallback query is empty too, the use case throws
 * {@link NoBedAvailableException}.
 */
@Service
public class EmergencyRecommendationService implements RecommendHospitalUseCase {

    private final HospitalSpecialtyRepository hospitalSpecialtyRepository;
    private final HospitalRepository hospitalRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DistanceCalculator distanceCalculator;
    private final BedReservationEventPublisher eventPublisher;
    private final RecommendationProperties properties;
    private final Clock clock;

    public EmergencyRecommendationService(HospitalSpecialtyRepository hospitalSpecialtyRepository,
                                          HospitalRepository hospitalRepository,
                                          SpecialtyRepository specialtyRepository,
                                          DistanceCalculator distanceCalculator,
                                          BedReservationEventPublisher eventPublisher,
                                          RecommendationProperties properties,
                                          Clock clock) {
        this.hospitalSpecialtyRepository = hospitalSpecialtyRepository;
        this.hospitalRepository = hospitalRepository;
        this.specialtyRepository = specialtyRepository;
        this.distanceCalculator = distanceCalculator;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Recommendation recommend(RecommendationQuery query) {
        List<HospitalSpecialty> specialtyCandidates =
                hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(query.specialtyId());
        if (!specialtyCandidates.isEmpty()) {
            Specialty requested = specialtyCandidates.get(0).specialty();
            return reserveNearest(query, specialtyCandidates, requested, false);
        }

        List<HospitalSpecialty> fallbackCandidates = hospitalSpecialtyRepository.findWithAnyAvailableBeds();
        if (fallbackCandidates.isEmpty()) {
            throw new NoBedAvailableException(query.specialtyId());
        }

        Specialty requested = specialtyRepository.findById(query.specialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(query.specialtyId()));
        return reserveNearest(query, fallbackCandidates, requested, true);
    }

    private Recommendation reserveNearest(RecommendationQuery query,
                                          List<HospitalSpecialty> candidates,
                                          Specialty requestedSpecialty,
                                          boolean fallback) {
        List<Scored> ranked = candidates.stream()
                .map(row -> {
                    Hospital hospital = hospitalRepository.findById(row.hospitalId())
                            .orElseThrow(() -> new HospitalNotFoundException(row.hospitalId()));
                    RouteInfo route = distanceCalculator.calculate(query.origin(), hospital.coordinates());
                    return new Scored(row, hospital, route);
                })
                .sorted(Comparator.<Scored>comparingDouble(scored -> scored.route().distanceKm())
                        .thenComparingLong(scored -> scored.hospital().id()))
                .toList();

        int maxAttempts = Math.min(properties.maxReservationAttempts(), ranked.size());
        OptimisticLockConflictException lastConflict = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Scored scored = ranked.get(attempt);
            try {
                HospitalSpecialty reserved = hospitalSpecialtyRepository.reserveBed(scored.row());
                BedReservationEvent event = new BedReservationEvent(
                        scored.hospital().id(),
                        scored.hospital().name(),
                        reserved.specialty().id(),
                        reserved.specialty().name(),
                        reserved.availableBeds(),
                        clock.instant()
                );
                eventPublisher.publish(event);
                return new Recommendation(
                        scored.hospital(),
                        reserved.specialty(),
                        requestedSpecialty,
                        reserved.availableBeds(),
                        scored.route(),
                        true,
                        fallback,
                        event.timestamp()
                );
            } catch (OptimisticLockConflictException conflict) {
                lastConflict = conflict;
            }
        }

        throw lastConflict;
    }

    private record Scored(HospitalSpecialty row, Hospital hospital, RouteInfo route) {
    }
}
