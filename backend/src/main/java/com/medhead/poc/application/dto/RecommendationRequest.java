package com.medhead.poc.application.dto;

/**
 * Request payload for {@code POST /api/v1/emergency/recommend}: the specialty
 * the patient needs and the GPS origin of the emergency. PR-S5-08 attaches
 * Bean Validation constraints to this record; until then, missing fields
 * surface as the framework's default deserialisation errors.
 */
public record RecommendationRequest(Long specialtyId, Double latitude, Double longitude) {
}
