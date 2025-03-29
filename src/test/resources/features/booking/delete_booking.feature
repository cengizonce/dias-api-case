Feature: Delete Booking

  Background:
    Given I have a valid auth token
    And I create a test booking with following data
      | firstname | lastname | checkin     | checkout    |
      | John      | Doe      | 2023-01-01  | 2023-01-10  |

  Scenario: Successfully delete a booking
    When I delete the booking with ID "created"
    Then the booking should be successfully deleted