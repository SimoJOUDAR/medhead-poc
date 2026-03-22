package com.medhead.poc.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spring context bootstrap for Cucumber acceptance scenarios. Boots the full
 * application on a random port against a disposable PostgreSQL container so
 * REST Assured can drive real HTTP calls end-to-end -- mirrors the lifecycle
 * of {@code AbstractIntegrationIT} without extending it so Cucumber's own
 * JUnit Platform engine stays the sole test runner for this context.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CucumberSpringConfiguration {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
