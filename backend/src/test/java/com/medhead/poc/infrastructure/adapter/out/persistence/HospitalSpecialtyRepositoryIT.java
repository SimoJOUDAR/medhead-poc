package com.medhead.poc.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class HospitalSpecialtyRepositoryIT extends AbstractIntegrationIT {

    private static final long FRED_BROOKS_ID = 1L;
    private static final long CARDIOLOGY_ID = 21L;

    @Autowired
    private HospitalSpecialtyRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFredBrooksCardiology() {
        jdbcTemplate.update("""
                UPDATE hospital_specialties
                   SET available_beds = 2, version = 0
                 WHERE hospital_id = ? AND specialty_id = ?
                """, FRED_BROOKS_ID, CARDIOLOGY_ID);
    }

    @AfterEach
    void restoreFredBrooksCardiology() {
        jdbcTemplate.update("""
                UPDATE hospital_specialties
                   SET available_beds = 2, version = 0
                 WHERE hospital_id = ? AND specialty_id = ?
                """, FRED_BROOKS_ID, CARDIOLOGY_ID);
    }

    @Test
    void findWithAnyAvailableBeds_shouldReturnEveryRowWithFreeBedsAcrossSpecialties() {
        var rows = repository.findWithAnyAvailableBeds();

        assertThat(rows)
                .as("fallback candidate set must span multiple hospitals and specialties")
                .isNotEmpty()
                .allSatisfy(row -> assertThat(row.availableBeds()).isPositive())
                .extracting(HospitalSpecialty::hospitalId)
                .contains(FRED_BROOKS_ID, 3L, 5L);
        assertThat(rows)
                .extracting(row -> row.specialty().id())
                .contains(CARDIOLOGY_ID, 54L);

        Integer expectedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hospital_specialties WHERE available_beds > 0",
                Integer.class);
        assertThat(rows).hasSize(expectedCount);
    }

    @Test
    void findWithAnyAvailableBeds_shouldExcludeRowsWithoutFreeBeds() {
        jdbcTemplate.update("""
                UPDATE hospital_specialties
                   SET available_beds = 0
                 WHERE hospital_id = ? AND specialty_id = ?
                """, FRED_BROOKS_ID, CARDIOLOGY_ID);

        var rows = repository.findWithAnyAvailableBeds();

        assertThat(rows)
                .noneMatch(row -> row.hospitalId() == FRED_BROOKS_ID
                        && row.specialty().id() == CARDIOLOGY_ID);
    }

    @Test
    void reserveBed_shouldDecrementAvailableBedsAndBumpVersion() {
        HospitalSpecialty before = loadFredBrooksCardiology();
        assertThat(before.availableBeds()).isEqualTo(2);
        assertThat(before.version()).isZero();

        HospitalSpecialty after = repository.reserveBed(before);

        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.availableBeds()).isEqualTo(1);
        assertThat(after.version()).isEqualTo(1L);

        Integer persistedBeds = jdbcTemplate.queryForObject(
                "SELECT available_beds FROM hospital_specialties WHERE id = ?",
                Integer.class, before.id());
        Long persistedVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM hospital_specialties WHERE id = ?",
                Long.class, before.id());
        assertThat(persistedBeds).isEqualTo(1);
        assertThat(persistedVersion).isEqualTo(1L);
    }

    @Test
    void reserveBed_shouldThrowOptimisticLockConflict_whenCallerHoldsStaleVersion() {
        HospitalSpecialty stale = loadFredBrooksCardiology();
        HospitalSpecialty staleCopy = new HospitalSpecialty(
                stale.id(), stale.hospitalId(), stale.specialty(), stale.availableBeds(), stale.version());

        repository.reserveBed(stale);

        assertThatThrownBy(() -> repository.reserveBed(staleCopy))
                .isInstanceOf(OptimisticLockConflictException.class);

        Integer persistedBeds = jdbcTemplate.queryForObject(
                "SELECT available_beds FROM hospital_specialties WHERE id = ?",
                Integer.class, stale.id());
        assertThat(persistedBeds)
                .as("second reservation must not decrement a row it already lost the race on")
                .isEqualTo(1);
    }

    private HospitalSpecialty loadFredBrooksCardiology() {
        return repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID).stream()
                .filter(row -> row.hospitalId() == FRED_BROOKS_ID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fred Brooks cardiology row missing from seed"));
    }
}
