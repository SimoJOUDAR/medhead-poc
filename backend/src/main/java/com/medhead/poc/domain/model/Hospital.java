package com.medhead.poc.domain.model;

public record Hospital(Long id, String name, GpsCoordinates coordinates, String address) {
}
