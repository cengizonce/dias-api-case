Feature: Update Booking

  Scenario: Successfully update a booking
    Given I have a valid auth token
    And I create a test booking with following data
      | firstname | lastname | checkin     | checkout    | totalprice | depositpaid | additionalneeds |
      | Jim       | Brown    | 2018-01-01  | 2019-01-01  | 111        | true        | Breakfast       |
    When I update the booking with ID "created" with the following data:
      | firstname | lastname | checkin     | checkout    | totalprice | depositpaid | additionalneeds |
      | James     | Brown    | 2023-01-30  | 2023-02-08  | 200        | false       | Lunch           |
    Then the booking should be successfully updated
    And the updated booking should contain the new data
      | firstname | lastname | totalprice | depositpaid | checkin     | checkout    | additionalneeds |
      | James     | Brown    | 200        | false       | 2023-02-01  | 2023-02-10  | Lunch           |