package com.medhead.poc.infrastructure.mapper;

import com.medhead.poc.domain.model.Specialty;
import com.medhead.poc.domain.model.SpecialtyGroup;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyEntity;
import com.medhead.poc.infrastructure.adapter.out.persistence.entity.SpecialtyGroupEntity;
import org.springframework.stereotype.Component;

@Component
public class SpecialtyMapper {

    public Specialty toDomain(SpecialtyEntity entity) {
        SpecialtyGroupEntity groupEntity = entity.getGroup();
        SpecialtyGroup group = groupEntity == null
                ? null
                : new SpecialtyGroup(groupEntity.getId(), groupEntity.getName());
        return new Specialty(entity.getId(), entity.getName(), group);
    }

    public SpecialtyEntity toEntity(Specialty specialty) {
        SpecialtyGroupEntity groupEntity = specialty.group() == null
                ? null
                : new SpecialtyGroupEntity(specialty.group().id(), specialty.group().name());
        return new SpecialtyEntity(specialty.id(), specialty.name(), groupEntity);
    }
}
