package com.medhead.poc.domain.exception;

/**
 * Signals that the external routing service (OSRM) could not be reached or
 * returned an unusable response: the adapter tried to compute a route and the
 * HTTP call failed outright (connection refused, read timeout, 5xx, malformed
 * body). Translated at the routing adapter boundary from Spring's
 * {@code RestClientException} so the application layer stays framework-agnostic
 * (E1); {@code GlobalExceptionHandler} maps it to HTTP 503 with code
 * {@code ROUTING_UNAVAILABLE} so clients can distinguish a degraded dependency
 * from a genuine internal error.
 */
public class RoutingServiceUnavailableException extends RuntimeException {

    public RoutingServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
