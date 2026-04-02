package com.medhead.poc.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.medhead.poc.domain.model.BedReservationEvent;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Step definitions driving the hospital-recommendation acceptance scenarios.
 * Resolves the specialty id by name through {@code GET /api/v1/specialties}
 * so scenarios can stay written in clinical language instead of leaking
 * database identifiers into Gherkin.
 */
public class EmergencyRecommendationSteps {

    private final TestContext context;
    private final JdbcTemplate jdbcTemplate;
    private final BedReservationEventRecorder eventRecorder;

    public EmergencyRecommendationSteps(TestContext context,
                                        JdbcTemplate jdbcTemplate,
                                        BedReservationEventRecorder eventRecorder) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.eventRecorder = eventRecorder;
    }

    @Given("{string} has {int} available beds for {string}")
    public void hospital_has_n_available_beds_for_specialty(String hospitalName, int expectedBeds, String specialtyName) {
        Integer beds = queryAvailableBeds(hospitalName, specialtyName);
        assertThat(beds)
                .as("available beds for %s / %s before the scenario acts", hospitalName, specialtyName)
                .isEqualTo(expectedBeds);
    }

    @Given("no hospital has available beds for {string}")
    public void no_hospital_has_available_beds_for(String specialtyName) {
        int updated = jdbcTemplate.update("""
                        UPDATE hospital_specialties hs
                           SET available_beds = 0,
                               version        = version + 1
                         WHERE hs.specialty_id = (SELECT id FROM specialties WHERE name = ?)
                        """,
                specialtyName);
        assertThat(updated)
                .as("every row for %s must be zeroed to force the fallback path", specialtyName)
                .isPositive();
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

    @Then("the recommendation is marked as a fallback")
    public void the_recommendation_is_marked_as_a_fallback() {
        assertThat(context.lastResponse().jsonPath().getBoolean("fallback")).isTrue();
    }

    @Then("the requested specialty is {string}")
    public void the_requested_specialty_is(String expectedName) {
        String actual = context.lastResponse().jsonPath().getString("requestedSpecialty.name");
        assertThat(actual).isEqualTo(expectedName);
    }

    @Then("the specialty served is not {string}")
    public void the_specialty_served_is_not(String unexpectedName) {
        String actual = context.lastResponse().jsonPath().getString("specialty.name");
        assertThat(actual).isNotEqualTo(unexpectedName);
    }

    @Then("the bed is reserved")
    public void the_bed_is_reserved() {
        assertThat(context.lastResponse().jsonPath().getBoolean("bedReserved")).isTrue();
    }

    @Then("{string} now has {int} available bed(s) for {string}")
    public void hospital_now_has_n_available_beds_for_specialty(String hospitalName, int expectedBeds, String specialtyName) {
        Integer beds = queryAvailableBeds(hospitalName, specialtyName);
        assertThat(beds)
                .as("available beds for %s / %s after the reservation", hospitalName, specialtyName)
                .isEqualTo(expectedBeds);
    }

    @Then("a bed-reservation event was published for {string} and {string} with {int} remaining bed(s)")
    public void a_bed_reservation_event_was_published(String hospitalName,
                                                     String specialtyName,
                                                     int expectedRemaining) {
        List<BedReservationEvent> events = eventRecorder.events();
        assertThat(events)
                .as("bed-reservation events captured during the scenario")
                .hasSize(1);
        BedReservationEvent event = events.get(0);
        assertThat(event.hospitalName()).isEqualTo(hospitalName);
        assertThat(event.specialtyName()).isEqualTo(specialtyName);
        assertThat(event.remainingBeds()).isEqualTo(expectedRemaining);
    }

    private Integer queryAvailableBeds(String hospitalName, String specialtyName) {
        return jdbcTemplate.queryForObject("""
                        SELECT hs.available_beds
                          FROM hospital_specialties hs
                          JOIN hospitals h  ON hs.hospital_id  = h.id
                          JOIN specialties s ON hs.specialty_id = s.id
                         WHERE h.name = ?
                           AND s.name = ?
                        """,
                Integer.class,
                hospitalName,
                specialtyName);
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
