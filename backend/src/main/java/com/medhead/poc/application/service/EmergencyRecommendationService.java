package com.medhead.poc.application.service;

import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.exception.NoSpecialtyMatchException;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.port.in.RecommendHospitalUseCase;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Core hospital-recommendation use case (PR-S5-05 slice). Selects the nearest
 * hospital with a free bed in the requested specialty. This PR deliberately
 * stops at selection: no bed reservation, no event emission, no fallback.
 * PR-S5-06 adds reservation + event; PR-S5-07 adds the fallback path.
 */
@Service
public class EmergencyRecommendationService implements RecommendHospitalUseCase {

    private final HospitalSpecialtyRepository hospitalSpecialtyRepository;
    private final HospitalRepository hospitalRepository;
    private final DistanceCalculator distanceCalculator;
    private final Clock clock;

    public EmergencyRecommendationService(HospitalSpecialtyRepository hospitalSpecialtyRepository,
                                          HospitalRepository hospitalRepository,
                                          DistanceCalculator distanceCalculator,
                                          Clock clock) {
        this.hospitalSpecialtyRepository = hospitalSpecialtyRepository;
        this.hospitalRepository = hospitalRepository;
        this.distanceCalculator = distanceCalculator;
        this.clock = clock;
    }

    @Override
    public Recommendation recommend(RecommendationQuery query) {
        List<HospitalSpecialty> candidates =
                hospitalSpecialtyRepository.findWithAvailableBedsForSpecialty(query.specialtyId());
        if (candidates.isEmpty()) {
            throw new NoSpecialtyMatchException(query.specialtyId());
        }

        record Scored(HospitalSpecialty row, Hospital hospital, RouteInfo route) {
        }

        Scored best = candidates.stream()
                .map(row -> {
                    Hospital hospital = hospitalRepository.findById(row.hospitalId())
                            .orElseThrow(() -> new HospitalNotFoundException(row.hospitalId()));
                    RouteInfo route = distanceCalculator.calculate(query.origin(), hospital.coordinates());
                    return new Scored(row, hospital, route);
                })
                .min(Comparator.<Scored>comparingDouble(scored -> scored.route().distanceKm())
                        .thenComparingLong(scored -> scored.hospital().id()))
                .orElseThrow(() -> new NoSpecialtyMatchException(query.specialtyId()));

        return new Recommendation(
                best.hospital(),
                best.row().specialty(),
                best.row().availableBeds(),
                best.route(),
                false,
                false,
                clock.instant()
        );
    }
}
