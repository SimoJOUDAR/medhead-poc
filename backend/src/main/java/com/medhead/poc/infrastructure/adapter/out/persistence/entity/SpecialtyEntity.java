package com.medhead.poc.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "specialties",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "group_id"})
)
public class SpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private SpecialtyGroupEntity group;

    protected SpecialtyEntity() {
    }

    public SpecialtyEntity(Long id, String name, SpecialtyGroupEntity group) {
        this.id = id;
        this.name = name;
        this.group = group;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SpecialtyGroupEntity getGroup() {
        return group;
    }
}
