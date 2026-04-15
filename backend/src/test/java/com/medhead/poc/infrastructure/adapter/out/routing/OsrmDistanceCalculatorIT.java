package com.medhead.poc.infrastructure.adapter.out.routing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.medhead.poc.domain.exception.RoutingServiceUnavailableException;
import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.infrastructure.config.OsrmConfig;
import com.medhead.poc.infrastructure.config.OsrmProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Narrow integration test for the OSRM adapter. Spins up a WireMock server in
 * place of a real {@code osrm-routed} process, asserts that the adapter hits
 * the expected URL shape ({@code lon,lat;lon,lat}), normalises the metres /
 * seconds payload into kilometres / minutes, and honours the configured read
 * timeout.
 */
class OsrmDistanceCalculatorIT {

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @AfterEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    private OsrmDistanceCalculator calculatorWithReadTimeoutMs(int readTimeoutMs) {
        OsrmProperties properties = new OsrmProperties("localhost", wireMock.port(), readTimeoutMs);
        RestClient restClient = new OsrmConfig().osrmRestClient(properties);
        return new OsrmDistanceCalculator(restClient);
    }

    @Test
    void calculate_shouldBuildLonLatPathAndNormaliseMetresAndSecondsIntoKmAndMinutes() {
        wireMock.stubFor(get(urlPathMatching("/route/v1/driving/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "Ok",
                                  "routes": [
                                    { "distance": 3240.5, "duration": 480.0 }
                                  ]
                                }
                                """)));

        GpsCoordinates origin = new GpsCoordinates(51.5230, -0.1310);
        GpsCoordinates destination = new GpsCoordinates(51.5230, -0.1300);

        RouteInfo route = calculatorWithReadTimeoutMs(2_000).calculate(origin, destination);

        assertThat(route.distanceKm()).isCloseTo(3.2405, within(0.0001));
        assertThat(route.travelTimeMinutes()).isCloseTo(8.0, within(0.0001));

        wireMock.verify(getRequestedFor(
                urlMatching("/route/v1/driving/-0\\.131000,51\\.523000;-0\\.130000,51\\.523000\\?overview=false")));
    }

    @Test
    void calculate_shouldWrapReadTimeoutAsRoutingServiceUnavailable() {
        wireMock.stubFor(get(urlPathMatching("/route/v1/driving/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(400)
                        .withBody("{\"code\":\"Ok\",\"routes\":[{\"distance\":1000,\"duration\":60}]}")));

        OsrmDistanceCalculator subject = calculatorWithReadTimeoutMs(100);

        assertThatThrownBy(() -> subject.calculate(
                new GpsCoordinates(51.5230, -0.1310),
                new GpsCoordinates(51.5230, -0.1300)))
                .isInstanceOf(RoutingServiceUnavailableException.class);
    }

    @Test
    void calculate_shouldWrapServerErrorAsRoutingServiceUnavailable() {
        wireMock.stubFor(get(urlPathMatching("/route/v1/driving/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"NoRoute\"}")));

        OsrmDistanceCalculator subject = calculatorWithReadTimeoutMs(2_000);

        assertThatThrownBy(() -> subject.calculate(
                new GpsCoordinates(51.5230, -0.1310),
                new GpsCoordinates(51.5230, -0.1300)))
                .isInstanceOf(RoutingServiceUnavailableException.class);
    }

    @Test
    void calculate_shouldWrapEmptyRoutesAsRoutingServiceUnavailable() {
        wireMock.stubFor(get(urlPathMatching("/route/v1/driving/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"Ok\",\"routes\":[]}")));

        OsrmDistanceCalculator subject = calculatorWithReadTimeoutMs(2_000);

        assertThatThrownBy(() -> subject.calculate(
                new GpsCoordinates(51.5230, -0.1310),
                new GpsCoordinates(51.5230, -0.1300)))
                .isInstanceOf(RoutingServiceUnavailableException.class);
    }
}
