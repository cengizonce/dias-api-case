Feature: Get Booking Details by ID (JSON)

  Background:
    Given I have a valid auth token
    And I create a test booking with following data
      | firstname | lastname | checkin     | checkout    |
      | John      | Doe      | 2023-01-01  | 2023-01-10  |

  Scenario: Get booking details with valid ID
    When I request details for booking ID "test_booking"
    Then the response status code should be 200
    And the response should contain valid JSON booking details
    And the response booking dates should be valid