package com.medhead.poc.domain.model;

/**
 * Input to the hospital-recommendation use case: the specialty the patient
 * requires and the origin point of the emergency.
 */
public record RecommendationQuery(Long specialtyId, GpsCoordinates origin) {
}
