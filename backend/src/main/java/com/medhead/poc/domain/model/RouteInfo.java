package com.medhead.poc.domain.model;

/**
 * Road-based route between two {@link GpsCoordinates} points, as returned by
 * an OSRM-compatible routing engine. Values are normalised into kilometres and
 * minutes at the adapter boundary so the domain layer never sees raw metres or
 * seconds.
 */
public record RouteInfo(double distanceKm, double travelTimeMinutes) {
}
