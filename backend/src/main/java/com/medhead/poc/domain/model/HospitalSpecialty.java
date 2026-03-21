package com.medhead.poc.domain.model;

public record HospitalSpecialty(Long id, Long hospitalId, Specialty specialty, int availableBeds) {
}
