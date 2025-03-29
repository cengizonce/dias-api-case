Feature: API Health Check

  Scenario: Check API health status
    When I check the API health status
    Then the API is up and running