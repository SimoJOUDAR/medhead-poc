package com.medhead.poc.infrastructure.adapter.out.routing;

import com.medhead.poc.domain.model.GpsCoordinates;
import java.util.Locale;

/**
 * Builds the cache key used by {@link OsrmDistanceCalculator} for the
 * {@code osrm-distances} cache. Origin is rounded to a ~100 m grid
 * (3 decimal places) so coordinates within the same emergency staging area
 * collapse onto one cache slot; destination keeps full precision since
 * hospital coordinates are static and already serve as a stable hospital
 * identifier.
 */
public final class OsrmCacheKey {

    private OsrmCacheKey() {
    }

    public static String of(GpsCoordinates origin, GpsCoordinates destination) {
        return String.format(
                Locale.ROOT,
                "%.3f:%.3f:%.6f:%.6f",
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude());
    }
}
