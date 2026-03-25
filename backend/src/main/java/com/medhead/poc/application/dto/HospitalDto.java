package com.medhead.poc.application.dto;

import java.util.List;

/**
 * Flat projection of a hospital returned by {@code GET /api/v1/hospitals} and
 * {@code GET /api/v1/hospitals/&#123;id&#125;}. Carries identity, location and
 * address fields alongside a denormalised list of the specialties the hospital
 * offers with their current bed availability -- one request is enough to
 * populate a network overview without a second round-trip.
 */
public record HospitalDto(
        Long id,
        String name,
        double latitude,
        double longitude,
        String address,
        List<HospitalSpecialtySummaryDto> specialties
) {

    public record HospitalSpecialtySummaryDto(
            Long specialtyId,
            String specialtyName,
            int availableBeds
    ) {
    }
}
