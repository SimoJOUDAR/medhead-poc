package com.medhead.poc.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.domain.model.GpsCoordinates;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.HospitalEntity;
import org.junit.jupiter.api.Test;

class HospitalMapperTest {

    private final HospitalMapper mapper = new HospitalMapper();

    @Test
    void toDomain_shouldPreserveEveryField() {
        HospitalEntity entity = new HospitalEntity(
                1L,
                "Fred Brooks Hospital",
                51.5230,
                -0.1300,
                "123 Computing Avenue, London WC1E 6BT"
        );

        Hospital hospital = mapper.toDomain(entity);

        assertThat(hospital.id()).isEqualTo(1L);
        assertThat(hospital.name()).isEqualTo("Fred Brooks Hospital");
        assertThat(hospital.coordinates()).isEqualTo(new GpsCoordinates(51.5230, -0.1300));
        assertThat(hospital.address()).isEqualTo("123 Computing Avenue, London WC1E 6BT");
    }

    @Test
    void domainToEntityAndBack_shouldRoundTripEveryField() {
        Hospital original = new Hospital(
                7L,
                "Charles Babbage Hospital",
                new GpsCoordinates(51.4800, -0.0700),
                "64 Difference Engine Road, London SE10 9NF"
        );

        Hospital roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
    }
}
