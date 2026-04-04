package com.medhead.poc.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Step definitions for the recommendation error-path scenarios. Keeps the
 * phrasing distinct from {@link EmergencyRecommendationSteps} so steps that
 * intentionally post malformed payloads (e.g. out-of-range coordinates) are
 * not funnelled through the specialty-id-resolving happy-path step, which
 * would mask the 400 under an AssertionError.
 */
public class RecommendationErrorSteps {

    private final TestContext context;
    private final JdbcTemplate jdbcTemplate;

    public RecommendationErrorSteps(TestContext context, JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("no hospital has any available beds")
    public void no_hospital_has_any_available_beds() {
        int updated = jdbcTemplate.update(
                "UPDATE hospital_specialties SET available_beds = 0, version = version + 1");
        assertThat(updated)
                .as("every hospital_specialties row must be zeroed to force the no-beds path")
                .isPositive();
    }

    @When("the user posts a recommendation with specialty {string}, latitude {double} and longitude {double}")
    public void the_user_posts_a_recommendation_with(String specialtyName, double latitude, double longitude) {
        Long specialtyId = resolveSpecialtyId(specialtyName);
        String body = String.format(
                "{\"specialtyId\":%d,\"latitude\":%s,\"longitude\":%s}",
                specialtyId, latitude, longitude);

        context.setLastResponse(
                RestAssured.given()
                        .baseUri(context.baseUrl())
                        .header("Authorization", "Bearer " + context.bearerToken())
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post("/api/v1/emergency/recommend")
        );
    }

    @Then("the error code is {string}")
    public void the_error_code_is(String expected) {
        String actual = context.lastResponse().jsonPath().getString("code");
        assertThat(actual).isEqualTo(expected);
    }

    @Then("the error details include field {string}")
    public void the_error_details_include_field(String expectedField) {
        List<Map<String, Object>> details = context.lastResponse().jsonPath().getList("details");
        assertThat(details)
                .as("error payload must carry per-field details")
                .isNotNull()
                .anyMatch(entry -> expectedField.equals(entry.get("field")));
    }

    private Long resolveSpecialtyId(String specialtyName) {
        List<Map<String, Object>> specialties = RestAssured.given()
                .baseUri(context.baseUrl())
                .header("Authorization", "Bearer " + context.bearerToken())
                .get("/api/v1/specialties")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        return specialties.stream()
                .filter(entry -> specialtyName.equals(entry.get("name")))
                .map(entry -> ((Number) entry.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Specialty not found in catalogue: " + specialtyName));
    }
}
