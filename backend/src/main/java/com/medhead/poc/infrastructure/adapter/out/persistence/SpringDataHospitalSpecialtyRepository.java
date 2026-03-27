package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalSpecialtyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataHospitalSpecialtyRepository extends JpaRepository<HospitalSpecialtyEntity, Long> {

    List<HospitalSpecialtyEntity> findBySpecialtyIdAndAvailableBedsGreaterThan(Long specialtyId, int threshold);
}
