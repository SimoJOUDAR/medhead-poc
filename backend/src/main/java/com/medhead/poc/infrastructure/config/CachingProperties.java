package com.medhead.poc.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for the {@code app.cache.*} configuration block. One {@link CacheSpec}
 * per cache, externalised so each Caffeine TTL and capacity can be tuned
 * without recompiling.
 */
@ConfigurationProperties(prefix = "app.cache")
public record CachingProperties(
        CacheSpec hospitalsBySpecialty,
        CacheSpec allHospitalsWithBeds,
        CacheSpec osrmDistances,
        CacheSpec specialties) {

    public record CacheSpec(long maximumSize, Duration expireAfterWrite) {

        public CacheSpec {
            if (maximumSize < 1) {
                throw new IllegalArgumentException(
                        "app.cache.*.maximum-size must be >= 1, got " + maximumSize);
            }
            if (expireAfterWrite == null || expireAfterWrite.isZero() || expireAfterWrite.isNegative()) {
                throw new IllegalArgumentException(
                        "app.cache.*.expire-after-write must be a positive duration, got " + expireAfterWrite);
            }
        }
    }
}
