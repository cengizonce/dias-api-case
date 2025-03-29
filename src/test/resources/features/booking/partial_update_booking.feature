Feature: Partial Update Booking

  Background:
    Given I have a valid auth token
    And I create a test booking with following data
      | firstname | lastname | checkin     | checkout    |
      | John      | Smith    | 2023-01-01  | 2023-01-10  |

  Scenario: Successfully update firstname and lastname of a booking
    When I partially update the created booking with:
      | field      | value  |
      | firstname  | James  |
      | lastname   | Brown  |
    Then the response status code should be 200
    And the updated booking should contain:
      | field      | value  |
      | firstname  | James  |
      | lastname   | Brown  |

  Scenario: Successfully update booking dates
    When I partially update the created booking with:
      | field      | value       |
      | bookingdates.checkin    | 2023-02-01 |
      | bookingdates.checkout   | 2023-02-10 |
    Then the response status code should be 200
    And the updated booking should contain:
      | field      | value       |
      | bookingdates.checkin    | 2023-02-01 |
      | bookingdates.checkout   | 2023-02-10 |

  Scenario: Successfully update additional needs
    When I partially update the created booking with:
      | field            | value      |
      | additionalneeds  | Breakfast  |
    Then the response status code should be 200
    And the updated booking should contain:
      | field            | value      |
      | additionalneeds  | Breakfast  |