package com.medhead.poc.infrastructure.adapter.out.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.domain.model.GpsCoordinates;
import org.junit.jupiter.api.Test;

class OsrmCacheKeyTest {

    @Test
    void of_shouldRoundOriginToThreeDecimalsAndKeepDestinationPrecision() {
        GpsCoordinates origin = new GpsCoordinates(51.523456, -0.131456);
        GpsCoordinates destination = new GpsCoordinates(51.523000, -0.130000);

        String key = OsrmCacheKey.of(origin, destination);

        assertThat(key).isEqualTo("51.523:-0.131:51.523000:-0.130000");
    }

    @Test
    void of_shouldCollapseOriginsWithinSameHundredMetreGridOntoOneKey() {
        GpsCoordinates origin1 = new GpsCoordinates(51.5230, -0.1310);
        GpsCoordinates origin2 = new GpsCoordinates(51.5234, -0.1314);
        GpsCoordinates destination = new GpsCoordinates(51.5230, -0.1300);

        assertThat(OsrmCacheKey.of(origin1, destination))
                .isEqualTo(OsrmCacheKey.of(origin2, destination));
    }

    @Test
    void of_shouldDifferWhenDestinationDiffersOnSubMetreScale() {
        GpsCoordinates origin = new GpsCoordinates(51.5230, -0.1310);
        GpsCoordinates destinationA = new GpsCoordinates(51.5230, -0.1300);
        GpsCoordinates destinationB = new GpsCoordinates(51.5230, -0.1301);

        assertThat(OsrmCacheKey.of(origin, destinationA))
                .isNotEqualTo(OsrmCacheKey.of(origin, destinationB));
    }
}
