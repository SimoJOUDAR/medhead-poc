package com.medhead.poc.application.dto;

import java.time.Instant;

/**
 * Response payload for {@code POST /api/v1/emergency/recommend}. Shape matches
 * the API contract in the architecture blueprint: the recommended hospital
 * with its route metrics, the specialty actually served, the reservation
 * outcome, and whether the fallback path was taken.
 */
public record RecommendationResponse(
        RecommendedHospitalDto hospital,
        RecommendedSpecialtyDto specialty,
        boolean bedReserved,
        boolean fallback,
        Instant timestamp
) {

    public record RecommendedHospitalDto(
            Long id,
            String name,
            double latitude,
            double longitude,
            String address,
            int availableBeds,
            double distanceKm,
            double estimatedTravelTimeMinutes
    ) {
    }

    public record RecommendedSpecialtyDto(Long id, String name, String group) {
    }
}
