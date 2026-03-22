package com.medhead.poc.application.dto;

/**
 * Flat projection of a specialty returned by {@code GET /api/v1/specialties}.
 * Carries the specialty's own identifiers plus a denormalised group name so
 * front-end selectors can group entries without a second round-trip.
 */
public record SpecialtyDto(
        Long id,
        String name,
        SpecialtyGroupDto group
) {

    public record SpecialtyGroupDto(Long id, String name) {
    }
}
