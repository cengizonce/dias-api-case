package org.example.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.methods.AuthMethods;

public class AuthSteps {
    private final AuthMethods authMethods = new AuthMethods();

    @Given("I have valid admin credentials")
    public void iHaveValidAdminCredentials() {
        authMethods.loadAdminCredentials();
    }

    @Given("I have invalid credentials username {string} and password {string}")
    public void iHaveInvalidCredentials(String username, String password) {
        authMethods.setInvalidCredentials(username, password);
    }

    @When("I request a new auth token")
    public void iRequestANewAuthToken() {
        authMethods.createToken();
    }

    @Then("the auth token is successfully generated")
    public void theAuthTokenIsSuccessfullyGenerated() {
        authMethods.verifyTokenIsGenerated();
    }

    @Then("the auth request should fail with status code {int}")
    public void theAuthRequestShouldFailWithStatusCode(int expectedStatusCode) {
        authMethods.verifyAuthRequestFailsWithStatusCode(expectedStatusCode);
    }
}