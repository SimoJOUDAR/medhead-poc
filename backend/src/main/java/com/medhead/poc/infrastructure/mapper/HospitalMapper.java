package com.medhead.poc.infrastructure.mapper;

import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalEntity;
import org.springframework.stereotype.Component;

@Component
public class HospitalMapper {

    public Hospital toDomain(HospitalEntity entity) {
        return new Hospital(
                entity.getId(),
                entity.getName(),
                new GpsCoordinates(entity.getLatitude(), entity.getLongitude()),
                entity.getAddress()
        );
    }

    public HospitalEntity toEntity(Hospital hospital) {
        return new HospitalEntity(
                hospital.id(),
                hospital.name(),
                hospital.coordinates().latitude(),
                hospital.coordinates().longitude(),
                hospital.address()
        );
    }
}
