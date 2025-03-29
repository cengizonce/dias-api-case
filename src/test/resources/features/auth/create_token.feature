Feature: Authentication

  Scenario: Create auth token with valid admin credentials
    Given I have valid admin credentials
    When I request a new auth token
    Then the auth token is successfully generated

  Scenario: Fail to create auth token with invalid credentials
    Given I have invalid credentials username "invalid" and password "invalid"
    When I request a new auth token
    Then the auth request should fail with status code 200