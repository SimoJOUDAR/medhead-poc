package com.medhead.poc.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that exercise the real schema and seed data
 * against a disposable PostgreSQL container. Spring Boot's {@link ServiceConnection}
 * rewires the datasource to the container, and {@code spring.sql.init.mode=always}
 * in the main profile runs {@code schema.sql} and {@code data.sql} at startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationIT {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
