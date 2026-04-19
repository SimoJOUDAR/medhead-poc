package com.medhead.poc.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.model.HospitalSpecialty;
import com.medhead.poc.domain.port.out.HospitalSpecialtyRepository;
import com.medhead.poc.infrastructure.config.CachingConfig;
import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

class BedReservationCacheEvictionIT extends AbstractIntegrationIT {

    private static final long FRED_BROOKS_ID = 1L;
    private static final long CARDIOLOGY_ID = 21L;

    @Autowired
    private HospitalSpecialtyRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFredBrooksCardiologyAndClearCaches() {
        jdbcTemplate.update("""
                UPDATE hospital_specialties
                   SET available_beds = 2, version = 0
                 WHERE hospital_id = ? AND specialty_id = ?
                """, FRED_BROOKS_ID, CARDIOLOGY_ID);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
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
    void reserveBed_shouldEvictHospitalsBySpecialtyEntryForReservedSpecialty() {
        repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID);
        Cache hospitalsBySpecialty = cacheManager.getCache(CachingConfig.HOSPITALS_BY_SPECIALTY);
        assertThat(hospitalsBySpecialty.get(CARDIOLOGY_ID))
                .as("first read must populate hospitals-by-specialty[%s]", CARDIOLOGY_ID)
                .isNotNull();

        HospitalSpecialty fred = loadFredBrooksCardiology();
        repository.reserveBed(fred);

        assertThat(hospitalsBySpecialty.get(CARDIOLOGY_ID))
                .as("successful reservation must evict the cardiology entry")
                .isNull();
    }

    @Test
    void reserveBed_shouldEvictAllHospitalsWithBedsCacheEntirely() {
        repository.findWithAnyAvailableBeds();
        Cache allHospitalsWithBeds = cacheManager.getCache(CachingConfig.ALL_HOSPITALS_WITH_BEDS);
        assertThat(allHospitalsWithBeds.get("all"))
                .as("first read must populate all-hospitals-with-beds[all]")
                .isNotNull();

        HospitalSpecialty fred = loadFredBrooksCardiology();
        repository.reserveBed(fred);

        assertThat(allHospitalsWithBeds.get("all"))
                .as("successful reservation must clear the fallback-set cache")
                .isNull();
    }

    @Test
    void reserveBed_shouldNotEvictWhenOptimisticLockFails() {
        HospitalSpecialty fred = loadFredBrooksCardiology();
        HospitalSpecialty staleCopy = new HospitalSpecialty(
                fred.id(), fred.hospitalId(), fred.specialty(), fred.availableBeds(), fred.version());

        repository.reserveBed(fred);

        repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID);
        repository.findWithAnyAvailableBeds();
        Cache hospitalsBySpecialty = cacheManager.getCache(CachingConfig.HOSPITALS_BY_SPECIALTY);
        Cache allHospitalsWithBeds = cacheManager.getCache(CachingConfig.ALL_HOSPITALS_WITH_BEDS);
        assertThat(hospitalsBySpecialty.get(CARDIOLOGY_ID)).isNotNull();
        assertThat(allHospitalsWithBeds.get("all")).isNotNull();

        assertThatThrownBy(() -> repository.reserveBed(staleCopy))
                .isInstanceOf(OptimisticLockConflictException.class);

        assertThat(hospitalsBySpecialty.get(CARDIOLOGY_ID))
                .as("a failed reservation must leave the hospitals-by-specialty cache intact")
                .isNotNull();
        assertThat(allHospitalsWithBeds.get("all"))
                .as("a failed reservation must leave the all-hospitals-with-beds cache intact")
                .isNotNull();
    }

    @Test
    void reserveBed_shouldNotEvictOsrmDistancesOrSpecialtiesCaches() {
        Cache osrmDistances = cacheManager.getCache(CachingConfig.OSRM_DISTANCES);
        Cache specialties = cacheManager.getCache(CachingConfig.SPECIALTIES);
        osrmDistances.put("seed:key", "seed-value");
        specialties.put("all", "seed-value");

        HospitalSpecialty fred = loadFredBrooksCardiology();
        repository.reserveBed(fred);

        assertThat(osrmDistances.get("seed:key").get())
                .as("bed reservation must not evict osrm-distances (hospital coordinates are static)")
                .isEqualTo("seed-value");
        assertThat(specialties.get("all").get())
                .as("bed reservation must not evict specialties (reference data is unchanged)")
                .isEqualTo("seed-value");
    }

    private HospitalSpecialty loadFredBrooksCardiology() {
        return repository.findWithAvailableBedsForSpecialty(CARDIOLOGY_ID).stream()
                .filter(row -> row.hospitalId() == FRED_BROOKS_ID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fred Brooks cardiology row missing from seed"));
    }
}
