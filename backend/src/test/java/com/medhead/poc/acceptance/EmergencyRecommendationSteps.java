package com.medhead.poc.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;

/**
 * Step definitions driving the hospital-recommendation acceptance scenarios.
 * Resolves the specialty id by name through {@code GET /api/v1/specialties}
 * so scenarios can stay written in clinical language instead of leaking
 * database identifiers into Gherkin.
 */
public class EmergencyRecommendationSteps {

    private final TestContext context;

    public EmergencyRecommendationSteps(TestContext context) {
        this.context = context;
    }

    @When("the user requests a recommendation for {string} near latitude {double} longitude {double}")
    public void the_user_requests_a_recommendation(String specialtyName, double latitude, double longitude) {
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

    @Then("the recommended hospital is {string}")
    public void the_recommended_hospital_is(String expectedName) {
        String actual = context.lastResponse().jsonPath().getString("hospital.name");
        assertThat(actual).isEqualTo(expectedName);
    }

    @Then("the specialty served is {string}")
    public void the_specialty_served_is(String expectedName) {
        String actual = context.lastResponse().jsonPath().getString("specialty.name");
        assertThat(actual).isEqualTo(expectedName);
    }

    @Then("the recommendation is not marked as a fallback")
    public void the_recommendation_is_not_a_fallback() {
        assertThat(context.lastResponse().jsonPath().getBoolean("fallback")).isFalse();
    }

    @Then("the bed is not yet reserved")
    public void the_bed_is_not_yet_reserved() {
        assertThat(context.lastResponse().jsonPath().getBoolean("bedReserved")).isFalse();
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
