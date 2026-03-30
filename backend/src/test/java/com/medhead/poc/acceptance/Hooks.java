package com.medhead.poc.acceptance;

import io.cucumber.java.Before;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Per-scenario setup for the Cucumber suite. Re-applies {@code data.sql} so
 * every scenario starts from the canonical seed state -- critical now that
 * {@code recommend-main-scenario.feature} mutates {@code hospital_specialties}
 * by reserving a bed. The root INSERTs are idempotent
 * ({@code ON CONFLICT DO UPDATE}) which also resets the {@code version}
 * column, so optimistic-lock counters don't drift across scenarios.
 */
public class Hooks {

    private final DataSource dataSource;
    private final BedReservationEventRecorder eventRecorder;

    public Hooks(DataSource dataSource, BedReservationEventRecorder eventRecorder) {
        this.dataSource = dataSource;
        this.eventRecorder = eventRecorder;
    }

    @Before
    public void resetSeedData() throws Exception {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
        eventRecorder.reset();
    }
}
