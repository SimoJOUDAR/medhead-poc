package com.medhead.poc.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that exercise the real schema and seed data
 * against a disposable PostgreSQL container. The container is started exactly
 * once per test JVM via a static initializer and kept alive for the rest of
 * the run -- the "singleton container" pattern from the Testcontainers docs.
 *
 * <p>Using {@code @Testcontainers} / {@code @Container} instead would bind the
 * container lifecycle to each extending test class, restarting Postgres on a
 * fresh random port per class. Spring's context cache is coarser-grained than
 * that: two IT classes with identical configuration share a cached context,
 * whose HikariCP pool is pinned to whichever port the container had when the
 * context was first built. A subsequent restart on a new port strands that
 * cached pool on a dead address -- surfacing as a 30 s HikariCP timeout
 * followed by {@code Connection refused}. Starting once, statically, keeps
 * the port stable for the lifetime of the JVM and sidesteps the trap. Ryuk
 * tears the container down at JVM exit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationIT {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
