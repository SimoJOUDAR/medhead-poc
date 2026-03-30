package com.medhead.poc.infrastructure.mapper;

import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalSpecialtyEntity;
import org.springframework.stereotype.Component;

@Component
public class HospitalSpecialtyMapper {

    private final SpecialtyMapper specialtyMapper;

    public HospitalSpecialtyMapper(SpecialtyMapper specialtyMapper) {
        this.specialtyMapper = specialtyMapper;
    }

    public HospitalSpecialty toDomain(HospitalSpecialtyEntity entity) {
        return new HospitalSpecialty(
                entity.getId(),
                entity.getHospital().getId(),
                specialtyMapper.toDomain(entity.getSpecialty()),
                entity.getAvailableBeds(),
                entity.getVersion()
        );
    }
}
