package com.medhead.poc.domain.port.in;

import com.medhead.poc.domain.model.Recommendation;
import com.medhead.poc.domain.model.RecommendationQuery;

/**
 * Driving port for the core PoC behaviour: given a specialty and an emergency
 * origin, return the nearest hospital that can receive the patient. Exposed to
 * the web layer via {@code POST /api/v1/emergency/recommend}.
 */
public interface RecommendHospitalUseCase {

    Recommendation recommend(RecommendationQuery query);
}
