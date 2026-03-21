package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalSpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataHospitalSpecialtyRepository extends JpaRepository<HospitalSpecialtyEntity, Long> {
}
