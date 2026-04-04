package com.medhead.poc.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code POST /api/v1/emergency/recommend}: the specialty
 * the patient needs and the GPS origin of the emergency. Bean Validation
 * constraints rejected at the controller boundary (E4) -- missing fields land
 * as 400 through {@link
 * com.medhead.poc.infrastructure.adapter.in.web.GlobalExceptionHandler}.
 */
public record RecommendationRequest(
        @NotNull Long specialtyId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
}
