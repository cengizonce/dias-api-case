package org.example.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.methods.AuthMethods;
import org.example.methods.BookingMethods;
import org.example.models.BookingRequest;
import org.junit.Assert;

import java.text.ParseException;
import java.util.*;

public class BookingSteps {
    private final BookingMethods bookingMethods = new BookingMethods();
    private final AuthMethods authMethods = new AuthMethods();
    private String token;
    private BookingRequest currentBookingRequest;
    private Map<String, String> testBookingData;

    @Given("I have a valid auth token")
    public void iHaveAValidAuthToken() {
        this.token = authMethods.generateValidToken();
    }

    @When("I request all booking IDs")
    public void iRequestAllBookingIDs() {
        bookingMethods.getBookingIds();
    }

    @Then("I receive a list of booking IDs")
    public void iReceiveAListOfBookingIDs() {
        bookingMethods.verifyBookingIdsListNotEmpty();
    }

    @Given("I create a basic booking with {string}, {string}, {string}, and {string}")
    public void createBasicBooking(String firstname, String lastname, String checkin, String checkout) throws ParseException {
        bookingMethods.createBasicBooking(firstname, lastname, checkin, checkout);
    }

    @Then("the booking is successfully created")
    public void theBookingIsSuccessfullyCreated() {
        bookingMethods.verifyBookingIsCreated();
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {
        bookingMethods.verifyStatusCode(expectedStatusCode);
    }

    @Then("the response should contain an array of booking IDs")
    public void theResponseShouldContainAnArrayOfBookingIDs() {
        bookingMethods.verifyBookingIdsListNotEmpty();
    }

    @When("I request booking IDs filtered by firstname {string} and lastname {string}")
    public void iRequestBookingIDsFilteredByName(String firstname, String lastname) {
        bookingMethods.getBookingIdsWithFilters(firstname, lastname);
    }

    @Then("the response should contain the created booking ID")
    public void theResponseShouldContainTheCreatedBookingID() {
        bookingMethods.verifyResponseContainsCreatedBookingId();
    }

    @Then("the response should only include bookings matching the name filter")
    public void theResponseShouldOnlyIncludeBookingsMatchingTheNameFilter() {
        bookingMethods.verifyFilteredBookingsMatchName();
    }

    @When("I request booking IDs filtered by checkin {string} and checkout {string}")
    public void iRequestBookingIDsFilteredByDates(String checkin, String checkout) {
        bookingMethods.getBookingIdsWithDateFilters(checkin, checkout);
    }

    @Then("the response should only include bookings within the date range")
    public void theResponseShouldOnlyIncludeBookingsWithinDateRange() throws Exception {
        bookingMethods.verifyFilteredBookingsMatchDates();
    }

    @Then("the response should be an empty array")
    public void theResponseShouldBeAnEmptyArray() {
        bookingMethods.verifyResponseIsEmptyArray();
    }

    @Given("I create a test booking with following data")
    public void createTestBooking(DataTable dataTable) throws ParseException {
        bookingMethods.createTestBooking(dataTable);
    }

    @When("I request details for booking ID {string}")
    public void requestBookingDetails(String idType) {
        bookingMethods.getBookingByIdType(idType);
    }

    @Then("the response should match the test booking data")
    public void verifyResponseMatchesTestData() {
        bookingMethods.verifyBookingDetailsMatch(testBookingData);
    }

    @Then("the response booking dates should be valid")
    public void verifyBookingDatesAreValid() {
        bookingMethods.verifyBookingDatesValid();
    }

    @When("I update the booking with ID {int} with the following data:")
    public void iUpdateTheBookingWithFollowingData(int id, Map<String, String> bookingData) throws ParseException {
        bookingMethods.updateBookingWithMap(id, bookingData, token);
    }

    @Then("the updated booking should contain:")
    public void verifyUpdatedBooking(DataTable dataTable) {
        bookingMethods.verifyUpdatedBookingFromDataTable(dataTable);
    }

    @When("I partially update the created booking with:")
    public void iPartiallyUpdateTheCreatedBookingWith(DataTable dataTable) {
        bookingMethods.partialUpdateCreatedBooking(dataTable, token);
    }

    @When("I delete the booking with ID {string}")
    public void iDeleteTheBookingWithID(String idType) {
        bookingMethods.deleteBookingByIdType(idType, token);
    }

    @Then("the booking should be successfully deleted")
    public void theBookingShouldBeSuccessfullyDeleted() {
        bookingMethods.verifyBookingDeleted();
    }

    @Then("the response should contain valid JSON booking details")
    public void verifyValidBookingDetails() {
        bookingMethods.verifyValidBookingDetails();
    }

    @Given("I prepare booking data")
    public void prepareBookingData(DataTable dataTable) throws ParseException {
        bookingMethods.prepareBookingDataFromDataTable(dataTable);
    }

    @When("I create a booking with the prepared data")
    public void createBookingWithPreparedData() {
        bookingMethods.createCurrentBooking();
    }

    @When("I create the booking")
    public void createBooking() {
        bookingMethods.createBooking();
    }

    @Then("the response should contain a booking ID")
    public void verifyBookingIdInResponse() {
        bookingMethods.verifyBookingIdInResponse();
    }

    @Then("the response should match the request data")
    public void verifyResponseMatchesRequest() {
        bookingMethods.verifyResponseMatchesRequest();
    }

    @When("I update the booking with ID {string} with the following data:")
    public void updateBookingWithId(String idType, DataTable dataTable) throws ParseException {
        bookingMethods.updateBookingByIdTypeWithData(idType, dataTable, token);
    }

    @Then("the booking should be successfully updated")
    public void verifyBookingSuccessfullyUpdated() {
        bookingMethods.verifyStatusCode(200);
    }

    @Then("the updated booking should contain the new data")
    public void verifyUpdatedBookingData(DataTable dataTable) {
        bookingMethods.verifyUpdatedBookingFromDataTable(dataTable);
    }

    @Then("the response should contain a valid booking ID")
    public void theResponseShouldContainAValidBookingID() {
        int bookingId = bookingMethods.getBookingId();
        Assert.assertTrue("Booking ID should be greater than 0", bookingId > 0);
    }
}