package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSpecialtyRepository extends JpaRepository<SpecialtyEntity, Long> {
}
