package com.medhead.poc.domain.port.out;

import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.RouteInfo;

/**
 * Driven port computing the road-based distance and travel time between two
 * points. Backed by an OSRM HTTP adapter in production; replaced with a
 * deterministic test double in integration and acceptance tests.
 */
public interface DistanceCalculator {

    RouteInfo calculate(GpsCoordinates origin, GpsCoordinates destination);
}
