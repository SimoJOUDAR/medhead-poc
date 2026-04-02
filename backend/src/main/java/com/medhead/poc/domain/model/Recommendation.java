package com.medhead.poc.domain.model;

import java.time.Instant;

/**
 * Outcome of the hospital-recommendation use case. Carries the selected
 * hospital, the specialty actually served, the specialty originally requested
 * by the caller (distinct from {@code specialty} only when the fallback path
 * kicks in), the distance and estimated travel time to reach the hospital,
 * whether a bed was reserved as part of the recommendation, and whether the
 * fallback path was taken.
 */
public record Recommendation(
        Hospital hospital,
        Specialty specialty,
        Specialty requestedSpecialty,
        int availableBeds,
        RouteInfo route,
        boolean bedReserved,
        boolean fallback,
        Instant timestamp
) {
}
