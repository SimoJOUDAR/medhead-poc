package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.port.out.SpecialtyRepository;
import com.medhead.poc.infrastructure.mapper.SpecialtyMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSpecialtyRepository implements SpecialtyRepository {

    private final SpringDataSpecialtyRepository delegate;
    private final SpecialtyMapper mapper;

    public JpaSpecialtyRepository(SpringDataSpecialtyRepository delegate, SpecialtyMapper mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public List<Specialty> findAll() {
        return delegate.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
