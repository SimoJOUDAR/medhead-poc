package com.medhead.poc.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the shared {@link RestClient} that
 * {@link com.medhead.poc.infrastructure.adapter.out.routing.OsrmDistanceCalculator}
 * uses to reach the OSRM server. Base URL and read timeout are driven by the
 * {@link OsrmProperties} binding.
 */
@Configuration
@EnableConfigurationProperties(OsrmProperties.class)
public class OsrmConfig {

    @Bean
    public RestClient osrmRestClient(OsrmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        factory.setConnectTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        return RestClient.builder()
                .baseUrl("http://" + properties.host() + ":" + properties.port())
                .requestFactory(factory)
                .build();
    }
}
