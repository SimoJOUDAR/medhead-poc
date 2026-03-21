package com.medhead.poc.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalEntity;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalSpecialtyEntity;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyEntity;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyGroupEntity;
import org.junit.jupiter.api.Test;

class HospitalSpecialtyMapperTest {

    private final HospitalSpecialtyMapper mapper = new HospitalSpecialtyMapper(new SpecialtyMapper());

    @Test
    void toDomain_shouldPreserveEveryFieldIncludingNestedSpecialtyAndGroup() {
        HospitalEntity hospitalEntity = new HospitalEntity(
                1L,
                "Fred Brooks Hospital",
                51.5230,
                -0.1300,
                "123 Computing Avenue, London WC1E 6BT"
        );
        SpecialtyGroupEntity groupEntity = new SpecialtyGroupEntity(5L, "General medicine group");
        SpecialtyEntity specialtyEntity = new SpecialtyEntity(21L, "Cardiology", groupEntity);
        HospitalSpecialtyEntity entity = new HospitalSpecialtyEntity(
                42L,
                hospitalEntity,
                specialtyEntity,
                2
        );

        HospitalSpecialty domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(42L);
        assertThat(domain.hospitalId()).isEqualTo(1L);
        assertThat(domain.availableBeds()).isEqualTo(2);
        assertThat(domain.specialty().id()).isEqualTo(21L);
        assertThat(domain.specialty().name()).isEqualTo("Cardiology");
        assertThat(domain.specialty().group().id()).isEqualTo(5L);
        assertThat(domain.specialty().group().name()).isEqualTo("General medicine group");
    }

    @Test
    void toDomain_shouldHandleNullSpecialtyGroup() {
        HospitalEntity hospitalEntity = new HospitalEntity(
                9L,
                "Orphan Hospital",
                0.0,
                0.0,
                null
        );
        SpecialtyEntity specialtyEntity = new SpecialtyEntity(99L, "Other", null);
        HospitalSpecialtyEntity entity = new HospitalSpecialtyEntity(
                77L,
                hospitalEntity,
                specialtyEntity,
                0
        );

        HospitalSpecialty domain = mapper.toDomain(entity);

        assertThat(domain.specialty().group()).isNull();
        assertThat(domain.availableBeds()).isZero();
        assertThat(domain.hospitalId()).isEqualTo(9L);
    }
}
