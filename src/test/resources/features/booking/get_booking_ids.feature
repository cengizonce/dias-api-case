Feature: Booking ID Retrieval

  Background:
    Given I have a valid auth token

  Scenario: Get all booking IDs
    When I request all booking IDs
    Then I receive a list of booking IDs
    And the response status code should be 200
    And the response should contain an array of booking IDs

  Scenario: Get booking IDs filtered by firstname and lastname
    Given I create a basic booking with "John", "Doe", "2025-01-01", and "2025-01-10"
    And the booking is successfully created
    When I request booking IDs filtered by firstname "John" and lastname "Doe"
    Then the response status code should be 200
    And the response should contain the created booking ID
    And the response should only include bookings matching the name filter

  Scenario: Get booking IDs filtered by checkin and checkout dates
    Given I create a basic booking with "Jane", "Smith", "2025-02-15", and "2025-02-20"
    And the booking is successfully created
    When I request booking IDs filtered by checkin "2025-02-01" and checkout "2025-02-28"
    Then the response status code should be 200
    And the response should contain the created booking ID
    And the response should only include bookings within the date range

  Scenario: Get booking IDs with no matches
    When I request booking IDs filtered by firstname "NonExistent" and lastname "User"
    Then the response status code should be 200
    And the response should be an empty array