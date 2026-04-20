package com.medhead.poc.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.medhead.poc.infrastructure.config.CachingProperties.CacheSpec;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the four Caffeine caches that absorb the hot read paths. Each cache
 * has its own TTL and capacity bound to {@link CachingProperties} -- read
 * paths populate them, the bed-reservation write path evicts the
 * stock-sensitive entries, and OSRM / specialty caches expire purely on TTL.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CachingProperties.class)
public class CachingConfig {

    public static final String HOSPITALS_BY_SPECIALTY = "hospitals-by-specialty";
    public static final String ALL_HOSPITALS_WITH_BEDS = "all-hospitals-with-beds";
    public static final String OSRM_DISTANCES = "osrm-distances";
    public static final String SPECIALTIES = "specialties";

    @Bean
    public CacheManager cacheManager(CachingProperties properties) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                caffeineCache(HOSPITALS_BY_SPECIALTY, properties.hospitalsBySpecialty()),
                caffeineCache(ALL_HOSPITALS_WITH_BEDS, properties.allHospitalsWithBeds()),
                caffeineCache(OSRM_DISTANCES, properties.osrmDistances()),
                caffeineCache(SPECIALTIES, properties.specialties())
        ));
        return manager;
    }

    private static CaffeineCache caffeineCache(String name, CacheSpec spec) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(spec.maximumSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .recordStats()
                .build());
    }
}
