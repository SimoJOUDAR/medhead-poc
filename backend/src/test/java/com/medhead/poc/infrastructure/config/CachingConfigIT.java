package com.medhead.poc.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

class CachingConfigIT extends AbstractIntegrationIT {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cacheManager_shouldExposeFourCaffeineCachesDeclaredInArchitecture() {
        assertThat(cacheManager.getCacheNames())
                .containsExactlyInAnyOrder(
                        CachingConfig.HOSPITALS_BY_SPECIALTY,
                        CachingConfig.ALL_HOSPITALS_WITH_BEDS,
                        CachingConfig.OSRM_DISTANCES,
                        CachingConfig.SPECIALTIES);

        for (String name : cacheManager.getCacheNames()) {
            assertThat(cacheManager.getCache(name))
                    .as("cache %s must be backed by Caffeine", name)
                    .isInstanceOf(CaffeineCache.class);
        }
    }

    @Test
    void cacheManager_shouldRecordStatsForEveryCache() {
        for (String name : cacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(name);
            assertThat(cache).isNotNull();

            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

            String key = "stats-probe-" + name;
            nativeCache.put(key, "v");
            nativeCache.getIfPresent(key);
            nativeCache.getIfPresent("absent-" + name);
            nativeCache.invalidate(key);

            assertThat(nativeCache.stats().requestCount())
                    .as("cache %s must record hit/miss stats (Caffeine.recordStats enabled)", name)
                    .isPositive();
        }
    }
}
