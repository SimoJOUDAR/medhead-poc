package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataHospitalRepository extends JpaRepository<HospitalEntity, Long> {
}
