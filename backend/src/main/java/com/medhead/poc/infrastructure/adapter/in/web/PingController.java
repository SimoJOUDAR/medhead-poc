package com.medhead.poc.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Walking-skeleton smoke endpoint. Returns a literal {@code pong} payload so
 * the front-end has something to call through the Vite dev-proxy, proving the
 * full stack wires up end-to-end. Disposable -- expected to be replaced or
 * removed once real domain endpoints land.
 */
@RestController
public class PingController {

    @GetMapping("/api/v1/ping")
    public PingResponse ping() {
        return new PingResponse("pong");
    }

    public record PingResponse(String message) {
    }
}
