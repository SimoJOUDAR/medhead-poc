package com.medhead.poc.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for the {@code app.osrm.*} configuration block. Points the
 * {@link com.medhead.poc.infrastructure.adapter.out.routing.OsrmDistanceCalculator}
 * at a local OSRM HTTP server.
 */
@ConfigurationProperties(prefix = "app.osrm")
public record OsrmProperties(String host, int port, int readTimeoutMs) {
}
