package com.medhead.poc.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import java.util.List;
import java.util.Map;

/**
 * Step definitions for the hospital-catalogue acceptance scenarios. Reuses the
 * authentication and request steps from {@link SpecialtyCatalogueSteps} --
 * Cucumber glue is scanned package-wide, so common {@code Given/When/Then}
 * clauses are defined once and composed freely across feature files.
 */
public class HospitalCatalogueSteps {

    private final TestContext context;

    public HospitalCatalogueSteps(TestContext context) {
        this.context = context;
    }

    @Then("the response lists all {int} seeded hospitals")
    public void the_response_lists_all_seeded_hospitals(int expectedCount) {
        List<?> hospitals = context.lastResponse().jsonPath().getList("$");
        assertThat(hospitals).hasSize(expectedCount);
    }

    @Then("{string} offers {string} with {int} available beds")
    public void hospital_offers_specialty_with_beds(String hospitalName,
                                                    String specialtyName,
                                                    int expectedBeds) {
        List<Map<String, Object>> hospitals = context.lastResponse().jsonPath().getList("$");
        Map<String, Object> hospital = hospitals.stream()
                .filter(entry -> hospitalName.equals(entry.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Hospital not found in response: " + hospitalName));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> specialties = (List<Map<String, Object>>) hospital.get("specialties");

        Integer availableBeds = specialties.stream()
                .filter(entry -> specialtyName.equals(entry.get("specialtyName")))
                .map(entry -> (Integer) entry.get("availableBeds"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Specialty " + specialtyName + " not found at " + hospitalName));

        assertThat(availableBeds)
                .as("availableBeds for %s at %s", specialtyName, hospitalName)
                .isEqualTo(expectedBeds);
    }
}
