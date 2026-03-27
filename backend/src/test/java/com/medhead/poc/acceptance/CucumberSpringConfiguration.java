package com.medhead.poc.acceptance;

import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spring context bootstrap for Cucumber acceptance scenarios. Boots the full
 * application on a random port against a disposable PostgreSQL container so
 * REST Assured can drive real HTTP calls end-to-end -- mirrors the lifecycle
 * of {@code AbstractIntegrationIT} without extending it so Cucumber's own
 * JUnit Platform engine stays the sole test runner for this context.
 *
 * <p>The nested {@link FakeOsrmConfiguration} replaces the OSRM HTTP adapter
 * with a Haversine-based {@link DistanceCalculator}: scenarios exercise the
 * real selection logic without requiring a running {@code osrm-routed} Docker
 * container in CI, and the ordering stays deterministic against the seeded
 * fixture coordinates.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CucumberSpringConfiguration {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class FakeOsrmConfiguration {

        @Bean
        @Primary
        public DistanceCalculator fakeDistanceCalculator() {
            return FakeOsrmConfiguration::haversine;
        }

        private static RouteInfo haversine(GpsCoordinates origin, GpsCoordinates destination) {
            double earthRadiusKm = 6371.0;
            double dLat = Math.toRadians(destination.latitude() - origin.latitude());
            double dLon = Math.toRadians(destination.longitude() - origin.longitude());
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(origin.latitude()))
                    * Math.cos(Math.toRadians(destination.latitude()))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.asin(Math.sqrt(a));
            double distanceKm = earthRadiusKm * c;
            return new RouteInfo(distanceKm, distanceKm * 1.2);
        }
    }
}
