Feature: Create Booking

  Scenario: Create booking with valid data
    Given I prepare booking data
      | firstname | lastname | totalprice | depositpaid | checkin     | checkout    | additionalneeds |
      | John      | Doe      | 200        | true        | 2023-01-01  | 2023-01-10  | Breakfast       |
    When I create the booking
    Then the response status code should be 200
    And the response should contain a valid booking ID
    And the response should match the request data