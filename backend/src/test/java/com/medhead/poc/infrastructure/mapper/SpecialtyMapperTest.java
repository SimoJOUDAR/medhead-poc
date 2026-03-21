package com.medhead.poc.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyEntity;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyGroupEntity;
import org.junit.jupiter.api.Test;

class SpecialtyMapperTest {

    private final SpecialtyMapper mapper = new SpecialtyMapper();

    @Test
    void toDomain_shouldPreserveEveryField_includingNestedGroup() {
        SpecialtyGroupEntity groupEntity = new SpecialtyGroupEntity(5L, "General medicine group");
        SpecialtyEntity entity = new SpecialtyEntity(21L, "Cardiology", groupEntity);

        Specialty specialty = mapper.toDomain(entity);

        assertThat(specialty.id()).isEqualTo(21L);
        assertThat(specialty.name()).isEqualTo("Cardiology");
        assertThat(specialty.group()).isEqualTo(new SpecialtyGroup(5L, "General medicine group"));
    }

    @Test
    void toDomain_shouldReturnNullGroup_whenEntityGroupIsMissing() {
        SpecialtyEntity orphan = new SpecialtyEntity(99L, "Other", null);

        Specialty specialty = mapper.toDomain(orphan);

        assertThat(specialty.group()).isNull();
    }

    @Test
    void domainToEntityAndBack_shouldRoundTripEveryField() {
        Specialty original = new Specialty(
                54L,
                "Immunology",
                new SpecialtyGroup(8L, "Pathology group")
        );

        Specialty roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
    }
}
