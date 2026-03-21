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
        name = "hospital_specialties",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hospital_id", "specialty_id"})
)
public class HospitalSpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private SpecialtyEntity specialty;

    @Column(name = "available_beds", nullable = false)
    private int availableBeds;

    protected HospitalSpecialtyEntity() {
    }

    public HospitalSpecialtyEntity(Long id, HospitalEntity hospital, SpecialtyEntity specialty, int availableBeds) {
        this.id = id;
        this.hospital = hospital;
        this.specialty = specialty;
        this.availableBeds = availableBeds;
    }

    public Long getId() {
        return id;
    }

    public HospitalEntity getHospital() {
        return hospital;
    }

    public SpecialtyEntity getSpecialty() {
        return specialty;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }
}
