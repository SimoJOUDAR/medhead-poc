package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.application.dto.RecommendationRequest;
import com.medhead.poc.application.dto.RecommendationResponse;
import com.medhead.poc.application.dto.RecommendationResponse.RecommendedHospitalDto;
import com.medhead.poc.application.dto.RecommendationResponse.RecommendedSpecialtyDto;
import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;
import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.domain.port.in.RecommendHospitalUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recommends the nearest hospital that can receive an emergency. Authenticated
 * access only -- falls under the default {@code /api/v1/**} rule of the
 * security chain.
 */
@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencyController {

    private final RecommendHospitalUseCase recommendHospitalUseCase;

    public EmergencyController(RecommendHospitalUseCase recommendHospitalUseCase) {
        this.recommendHospitalUseCase = recommendHospitalUseCase;
    }

    @PostMapping("/recommend")
    public ResponseEntity<RecommendationResponse> recommend(@RequestBody RecommendationRequest request) {
        Recommendation recommendation = recommendHospitalUseCase.recommend(new RecommendationQuery(
                request.specialtyId(),
                new GpsCoordinates(request.latitude(), request.longitude())
        ));
        return ResponseEntity.ok(toResponse(recommendation));
    }

    private static RecommendationResponse toResponse(Recommendation recommendation) {
        return new RecommendationResponse(
                toHospitalDto(recommendation),
                toSpecialtyDto(recommendation.specialty()),
                recommendation.bedReserved(),
                recommendation.fallback(),
                recommendation.timestamp()
        );
    }

    private static RecommendedHospitalDto toHospitalDto(Recommendation recommendation) {
        return new RecommendedHospitalDto(
                recommendation.hospital().id(),
                recommendation.hospital().name(),
                recommendation.hospital().coordinates().latitude(),
                recommendation.hospital().coordinates().longitude(),
                recommendation.hospital().address(),
                recommendation.availableBeds(),
                recommendation.route().distanceKm(),
                recommendation.route().travelTimeMinutes()
        );
    }

    private static RecommendedSpecialtyDto toSpecialtyDto(Specialty specialty) {
        SpecialtyGroup group = specialty.group();
        return new RecommendedSpecialtyDto(
                specialty.id(),
                specialty.name(),
                group == null ? null : group.name()
        );
    }
}
