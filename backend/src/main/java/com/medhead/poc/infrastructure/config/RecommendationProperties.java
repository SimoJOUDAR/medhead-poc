package com.medhead.poc.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs for the emergency recommendation flow. Currently caps the
 * bed-reservation retry count when the optimistic-lock race loses against a
 * concurrent writer -- the service falls through to the next-nearest candidate
 * up to this bound before surrendering.
 */
@ConfigurationProperties("app.recommendation")
public record RecommendationProperties(int maxReservationAttempts) {

    public RecommendationProperties {
        if (maxReservationAttempts < 1) {
            throw new IllegalArgumentException(
                    "app.recommendation.max-reservation-attempts must be >= 1, got " + maxReservationAttempts);
        }
    }
}
