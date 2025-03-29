package org.example.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.example.methods.PingMethods;

public class PingSteps {
    private final PingMethods pingMethods = new PingMethods();
    private Response response;

    @When("I check the API health status")
    public void iCheckTheAPIHealthStatus() {
        response = pingMethods.healthCheck();
    }

    @Then("the API is up and running")
    public void theAPIIsUpAndRunning() {
        pingMethods.verifyApiIsRunning(response);
    }
}