package com.medhead.poc.infrastructure.adapter.out.persistence;

import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalSpecialtyEntity;
import com.medhead.poc.infrastructure.mapper.HospitalSpecialtyMapper;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public HospitalSpecialty reserveBed(HospitalSpecialty hospitalSpecialty) {
        HospitalSpecialtyEntity entity = delegate.findById(hospitalSpecialty.id())
                .orElseThrow(() -> new OptimisticLockConflictException(hospitalSpecialty.id()));

        if (entity.getVersion() != hospitalSpecialty.version()) {
            throw new OptimisticLockConflictException(hospitalSpecialty.id());
        }

        entity.decrementAvailableBeds();

        try {
            HospitalSpecialtyEntity saved = delegate.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (OptimisticLockingFailureException e) {
            throw new OptimisticLockConflictException(hospitalSpecialty.id(), e);
        }
    }
}
