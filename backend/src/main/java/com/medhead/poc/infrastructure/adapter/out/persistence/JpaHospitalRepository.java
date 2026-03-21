package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.out.HospitalRepository;
import com.medhead.poc.infrastructure.mapper.HospitalMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalRepository implements HospitalRepository {

    private final SpringDataHospitalRepository delegate;
    private final HospitalMapper mapper;

    public JpaHospitalRepository(SpringDataHospitalRepository delegate, HospitalMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<Hospital> findAll() {
        return delegate.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Hospital> findById(Long id) {
        return delegate.findById(id).map(mapper::toDomain);
    }
}
