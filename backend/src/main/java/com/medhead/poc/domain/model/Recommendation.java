package com.medhead.poc.domain.model;

import java.time.Instant;

/**
 * Outcome of the hospital-recommendation use case. Carries the selected
 * hospital and the specialty it serves, the distance and estimated travel time
 * to reach it, whether a bed was reserved as part of the recommendation
 * (always {@code false} until PR-S5-06 introduces reservation), and whether
 * the fallback path was taken (always {@code false} until PR-S5-07).
 */
public record Recommendation(
        Hospital hospital,
        Specialty specialty,
        int availableBeds,
        RouteInfo route,
        boolean bedReserved,
        boolean fallback,
        Instant timestamp
) {
}
