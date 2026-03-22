package com.medhead.poc.acceptance;

import io.cucumber.spring.ScenarioScope;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.stereotype.Component;

/**
 * Scenario-scoped acceptance-test helper. Exposes the random server port as a
 * base URI, lazily logs in via {@code POST /api/v1/auth/login} on first use
 * and caches the resulting Bearer token for the lifetime of the scenario, and
 * carries the most recent REST Assured {@link Response} across steps.
 */
@Component
@ScenarioScope
public class TestContext {

    @LocalServerPort
    private int port;

    private String bearerToken;
    private Response lastResponse;

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public String bearerToken() {
        if (bearerToken == null) {
            bearerToken = RestAssured.given()
                    .baseUri(baseUrl())
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"demo\",\"password\":\"demo\"}")
                    .post("/api/v1/auth/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("token");
        }
        return bearerToken;
    }

    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }

    public Response lastResponse() {
        return lastResponse;
    }
}
