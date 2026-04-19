package com.medhead.poc.infrastructure.config;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.medhead.poc.domain.port.in.ListSpecialtiesUseCase;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.adapter.out.persistence.SpringDataHospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.adapter.out.persistence.SpringDataSpecialtyRepository;
import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Spies on the Spring Data delegates that the cached driven-port adapters wrap,
 * so the assertion is "the underlying query was issued exactly once for two
 * adapter calls" -- proof the {@code @Cacheable} path skipped the delegate on
 * the second call.
 */
class CachingBehaviorIT extends AbstractIntegrationIT {

    private static final long CARDIOLOGY_ID = 21L;

    @MockitoSpyBean
    private SpringDataHospitalSpecialtyRepository hospitalSpecialtyDelegate;

    @MockitoSpyBean
    private SpringDataSpecialtyRepository specialtyDelegate;

    @Autowired
    private HospitalSpecialtyRepository repository;

    @Autowired
    private ListSpecialtiesUseCase listSpecialtiesUseCase;

    @Test
    void findWithAvailableBedsForSpecialty_shouldQueryOnceForRepeatedCallsOnSameKey() {
        repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID);
        repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID);
        repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID);

        verify(hospitalSpecialtyDelegate, times(1))
                .findBySpecialtyIdAndAvailableBedsGreaterThan(CARDIOLOGY_ID, 0);
    }

    @Test
    void findWithAnyAvailableBeds_shouldQueryOnceForRepeatedCalls() {
        repository.findWithAnyAvailableBeds();
        repository.findWithAnyAvailableBeds();
        repository.findWithAnyAvailableBeds();

        verify(hospitalSpecialtyDelegate, times(1)).findByAvailableBedsGreaterThan(0);
    }

    @Test
    void listSpecialties_shouldQueryOnceForRepeatedCalls() {
        listSpecialtiesUseCase.listAll();
        listSpecialtiesUseCase.listAll();
        listSpecialtiesUseCase.listAll();

        verify(specialtyDelegate, times(1)).findAll();
    }
}
