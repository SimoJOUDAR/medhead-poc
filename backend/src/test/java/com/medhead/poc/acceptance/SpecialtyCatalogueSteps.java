package com.medhead.poc.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import java.util.List;

/**
 * Step definitions for the specialty-catalogue acceptance scenarios. Drives
 * real HTTP traffic against the running application via REST Assured, using
 * the scenario-scoped {@link TestContext} for authentication and
 * response-sharing between steps.
 */
public class SpecialtyCatalogueSteps {

    private final TestContext context;

    public SpecialtyCatalogueSteps(TestContext context) {
        this.context = context;
    }

    @Given("an authenticated user")
    public void an_authenticated_user() {
        context.bearerToken();
    }

    @When("the user requests {string}")
    public void the_user_requests(String path) {
        context.setLastResponse(
                RestAssured.given()
                        .baseUri(context.baseUrl())
                        .header("Authorization", "Bearer " + context.bearerToken())
                        .get(path)
        );
    }

    @Then("the response status is {int}")
    public void the_response_status_is(int expected) {
        assertThat(context.lastResponse().statusCode()).isEqualTo(expected);
    }

    @Then("the response lists the full NHS specialty catalogue")
    public void the_response_lists_the_full_nhs_specialty_catalogue() {
        List<?> items = context.lastResponse().jsonPath().getList("$");
        assertThat(items).hasSize(80);
    }

    @Then("every entry carries an id, a name and a group")
    public void every_entry_carries_an_id_a_name_and_a_group() {
        List<Long> ids = context.lastResponse().jsonPath().getList("id", Long.class);
        List<String> names = context.lastResponse().jsonPath().getList("name", String.class);
        List<String> groupNames = context.lastResponse().jsonPath().getList("group.name", String.class);

        assertThat(ids).hasSize(80).allMatch(id -> id != null && id > 0);
        assertThat(names).hasSize(80).allSatisfy(n -> assertThat(n).isNotBlank());
        assertThat(groupNames).hasSize(80).allSatisfy(g -> assertThat(g).isNotBlank());
    }
}
