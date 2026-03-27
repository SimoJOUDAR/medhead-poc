package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.mapper.HospitalSpecialtyMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalSpecialtyRepository implements HospitalSpecialtyRepository {

    private final SpringDataHospitalSpecialtyRepository delegate;
    private final HospitalSpecialtyMapper mapper;

    public JpaHospitalSpecialtyRepository(SpringDataHospitalSpecialtyRepository delegate,
                                          HospitalSpecialtyMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<HospitalSpecialty> findAll() {
        return delegate.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<HospitalSpecialty> findWithAvailableBedsForSpecialty(Long specialtyId) {
        return delegate.findBySpecialtyIdAndAvailableBedsGreaterThan(specialtyId, 0).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
