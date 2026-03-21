package com.medhead.poc.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaLoadIT extends AbstractIntegrationIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaAndSeedData_shouldLoadCleanly_whenContainerBoots() {
        assertThat(POSTGRES.isRunning()).isTrue();

        Long groupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM specialty_groups", Long.class);
        Long specialtyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM specialties", Long.class);
        Long hospitalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hospitals", Long.class);
        Long hospitalSpecialtyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hospital_specialties", Long.class);

        assertThat(groupCount).isEqualTo(12L);
        assertThat(specialtyCount).isEqualTo(80L);
        assertThat(hospitalCount).isEqualTo(12L);
        assertThat(hospitalSpecialtyCount).isPositive();
    }

    @Test
    void fredBrooksCardiology_shouldHaveTwoAvailableBeds() {
        Integer beds = queryBedsByHospitalAndSpecialty("Fred Brooks Hospital", "Cardiology");

        assertThat(beds).isEqualTo(2);
    }

    @Test
    void juliaCrusherCardiology_shouldHaveZeroAvailableBeds() {
        Integer beds = queryBedsByHospitalAndSpecialty("Julia Crusher Hospital", "Cardiology");

        assertThat(beds).isZero();
    }

    @Test
    void beverlyBashir_shouldNotOfferCardiology() {
        Long rowCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                          FROM hospital_specialties hs
                          JOIN hospitals h  ON hs.hospital_id  = h.id
                          JOIN specialties s ON hs.specialty_id = s.id
                         WHERE h.name = ?
                           AND s.name = ?
                        """,
                Long.class,
                "Beverly Bashir Hospital",
                "Cardiology");

        assertThat(rowCount).isZero();
    }

    private Integer queryBedsByHospitalAndSpecialty(String hospitalName, String specialtyName) {
        return jdbcTemplate.queryForObject("""
                        SELECT hs.available_beds
                          FROM hospital_specialties hs
                          JOIN hospitals h  ON hs.hospital_id  = h.id
                          JOIN specialties s ON hs.specialty_id = s.id
                         WHERE h.name = ?
                           AND s.name = ?
                        """,
                Integer.class,
                hospitalName,
                specialtyName);
    }
}
