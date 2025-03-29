package org.example.methods;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.datatable.DataTable;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.TestConfig;
import org.example.context.ScenarioContext;
import org.example.models.BookingDates;
import org.example.models.BookingRequest;
import org.example.models.BookingResponse;
import org.junit.Assert;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BookingMethods {
    private static final Logger logger = LogManager.getLogger(BookingMethods.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MAX_VERIFICATION_ATTEMPTS = 3;
    private static final long VERIFICATION_RETRY_DELAY_MS = 1000;
    private static final String BOOKING_ENDPOINT = "/booking";
    private static final String CONTENT_TYPE = "application/json";
    private static final String TOKEN_HEADER = "Cookie";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String TOKEN_PREFIX = "token=";

    private final ScenarioContext scenarioContext = ScenarioContext.getInstance();
    private Response response;
    private int bookingId;
    private BookingRequest currentBookingRequest;

    public Response getBookingById(int id) {
        try {
            logger.info("Attempting to get booking by ID: {}", id);
            this.response = executeGetRequest(buildBookingPath(id));
            logger.info("Successfully retrieved booking with ID: {}", id);
            return response;
        } catch (Exception e) {
            logger.error("Failed to get booking by ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to get booking by ID", e);
        }
    }

    public void getBookingByIdType(String idType) {
        try {
            logger.info("Getting booking by ID type: {}", idType);
            int id = resolveBookingIdFromType(idType);
            this.response = getBookingById(id);
        } catch (Exception e) {
            logger.error("Failed to get booking by ID type {}: {}", idType, e.getMessage());
            throw e;
        }
    }

    private int resolveBookingIdFromType(String idType) {
        if (idType.equals("test_booking")) {
            return getTestBookingIdFromContext();
        }
        return parseBookingId(idType);
    }

    private int getTestBookingIdFromContext() {
        Object testBookingId = scenarioContext.getContext("test_booking_id");
        if (testBookingId == null) {
            String errorMsg = "Test booking ID not found in scenario context";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return (int) testBookingId;
    }

    private int parseBookingId(String idType) {
        try {
            return Integer.parseInt(idType);
        } catch (NumberFormatException e) {
            String errorMsg = "Invalid booking ID format: " + idType;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }

    public void createBooking() {
        if (this.currentBookingRequest == null) {
            throw new IllegalStateException("No booking request available");
        }
        this.response = executeBookingCreationRequest(this.currentBookingRequest);
        this.bookingId = extractBookingIdFromResponse();
    }

    private void validateBookingRequest(BookingRequest request) {
        if (request.getFirstname() == null || request.getFirstname().isEmpty()) {
            throw new IllegalArgumentException("Firstname cannot be null or empty");
        }
        if (request.getLastname() == null || request.getLastname().isEmpty()) {
            throw new IllegalArgumentException("Lastname cannot be null or empty");
        }
        if (request.getBookingdates() == null) {
            throw new IllegalArgumentException("Booking dates cannot be null");
        }
        if (request.getBookingdates().getCheckin() == null) {
            throw new IllegalArgumentException("Checkin date cannot be null");
        }
        if (request.getBookingdates().getCheckout() == null) {
            throw new IllegalArgumentException("Checkout date cannot be null");
        }
    }

    private Response executeBookingCreationRequest(BookingRequest request) {
        return RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .contentType("application/json")
                .body(request)
                .post("/booking");
    }


    private int extractBookingIdFromResponse() {
        return response.jsonPath().getInt("bookingid");
    }

    public Response updateBooking(int id, BookingRequest bookingRequest, String token) {
        try {
            logger.info("Updating booking with ID: {}", id);
            Map<String, Object> requestBody = createUpdateRequestBody(bookingRequest);
            this.response = executeUpdateRequest(id, token, requestBody);
            logResponseDetails(response);
            logger.info("Booking updated successfully");
            return response;
        } catch (Exception e) {
            logger.error("Failed to update booking: {}", e.getMessage());
            throw new RuntimeException("Failed to update booking", e);
        }
    }

    private Map<String, Object> createUpdateRequestBody(BookingRequest bookingRequest) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstname", bookingRequest.getFirstname());
        requestBody.put("lastname", bookingRequest.getLastname());
        requestBody.put("totalprice", bookingRequest.getTotalprice());
        requestBody.put("depositpaid", bookingRequest.isDepositpaid());
        requestBody.put("bookingdates", createBookingDatesMap(bookingRequest));
        requestBody.put("additionalneeds", bookingRequest.getAdditionalneeds());
        logger.debug("Update request body: {}", requestBody);
        return requestBody;
    }

    private Map<String, String> createBookingDatesMap(BookingRequest bookingRequest) {
        Map<String, String> bookingDates = new HashMap<>();
        bookingDates.put("checkin", formatDate(bookingRequest.getBookingdates().getCheckin()));
        bookingDates.put("checkout", formatDate(bookingRequest.getBookingdates().getCheckout()));
        return bookingDates;
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private Response executeUpdateRequest(int id, String token, Map<String, Object> requestBody) {
        return RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .contentType(CONTENT_TYPE)
                .header(ACCEPT_HEADER, CONTENT_TYPE)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                .body(requestBody)
                .put(buildBookingPath(id));
    }

    public void partialUpdateBooking(int id, Map<String, Object> fields, String token) {
        try {
            logger.info("Partially updating booking with ID: {}", id);
            processDateFields(fields);
            String requestBody = serializeFieldsToJson(fields);
            executePartialUpdateRequest(id, token, requestBody);
            logResponseDetails(response);
            logger.info("Booking partially updated successfully");
        } catch (JsonProcessingException e) {
            handleJsonProcessingError(e);
        } catch (Exception e) {
            handlePartialUpdateError(e);
        }
    }

    private void processDateFields(Map<String, Object> fields) {
        if (fields.containsKey("checkin") || fields.containsKey("checkout")) {
            Map<String, Object> bookingDates = new HashMap<>();
            if (fields.containsKey("checkin")) {
                bookingDates.put("checkin", fields.remove("checkin"));
            }
            if (fields.containsKey("checkout")) {
                bookingDates.put("checkout", fields.remove("checkout"));
            }
            fields.put("bookingdates", bookingDates);
        }
    }

    private String serializeFieldsToJson(Map<String, Object> fields) throws JsonProcessingException {
        return mapper.writeValueAsString(fields);
    }

    private void executePartialUpdateRequest(int id, String token, String requestBody) {
        this.response = RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .contentType(CONTENT_TYPE)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                .body(requestBody)
                .patch(buildBookingPath(id));
    }

    private void handleJsonProcessingError(JsonProcessingException e) {
        logger.error("JSON processing failed during partial update: {}", e.getMessage());
        throw new RuntimeException("Failed to process request body", e);
    }

    private void handlePartialUpdateError(Exception e) {
        logger.error("Failed to partially update booking: {}", e.getMessage());
        throw new RuntimeException("Failed to partially update booking", e);
    }

    public Response deleteBooking(int bookingId, String token) {
        try {
            logger.info("Deleting booking with ID: {}", bookingId);
            this.response = executeDeleteRequest(bookingId, token);
            logResponseDetails(response);
            logger.info("Booking deleted successfully");
            return response;
        } catch (Exception e) {
            logger.error("Failed to delete booking: {}", e.getMessage());
            throw new RuntimeException("Failed to delete booking", e);
        }
    }

    private Response executeDeleteRequest(int bookingId, String token) {
        return RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .header(ACCEPT_HEADER, CONTENT_TYPE)
                .header(TOKEN_HEADER, TOKEN_PREFIX + token)
                .delete(buildBookingPath(bookingId));
    }

    public void getBookingIds() {
        try {
            logger.info("Getting all booking IDs");
            this.response = executeGetRequest(BOOKING_ENDPOINT);
            logResponseDetails(response);
            logger.info("Successfully retrieved booking IDs");
        } catch (Exception e) {
            logger.error("Failed to get booking IDs: {}", e.getMessage());
            throw new RuntimeException("Failed to get booking IDs", e);
        }
    }

    public void getBookingIdsWithFilters(String firstname, String lastname) {
        try {
            logger.info("Getting booking IDs with filters - firstname: {}, lastname: {}", firstname, lastname);
            storeFilterParameters(firstname, lastname);
            this.response = executeFilteredGetRequest(firstname, lastname);
            logResponseDetails(response);
            logger.info("Successfully retrieved filtered booking IDs");
        } catch (Exception e) {
            logger.error("Failed to get filtered booking IDs: {}", e.getMessage());
            throw new RuntimeException("Failed to get filtered booking IDs", e);
        }
    }

    private void storeFilterParameters(String firstname, String lastname) {
        scenarioContext.setContext("filterFirstname", firstname);
        scenarioContext.setContext("filterLastname", lastname);
    }

    private Response executeFilteredGetRequest(String firstname, String lastname) {
        return RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .queryParam("firstname", firstname)
                .queryParam("lastname", lastname)
                .get(BOOKING_ENDPOINT);
    }

    public void getBookingIdsWithDateFilters(String checkin, String checkout) {
        try {
            logger.info("Getting booking IDs with date filters - checkin: {}, checkout: {}", checkin, checkout);
            storeDateFilterParameters(checkin, checkout);
            this.response = executeDateFilteredGetRequest(checkin, checkout);
            logResponseDetails(response);
            logger.info("Successfully retrieved booking IDs with date filters");
        } catch (Exception e) {
            logger.error("Failed to get booking IDs with date filters: {}", e.getMessage());
            throw new RuntimeException("Failed to get booking IDs with date filters", e);
        }
    }

    private void storeDateFilterParameters(String checkin, String checkout) {
        scenarioContext.setContext("filterCheckin", checkin);
        scenarioContext.setContext("filterCheckout", checkout);
    }

    private Response executeDateFilteredGetRequest(String checkin, String checkout) {
        return RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .queryParam("checkin", checkin)
                .queryParam("checkout", checkout)
                .get(BOOKING_ENDPOINT);
    }

    public void updateBookingWithMap(int id, Map<String, String> bookingData, String token) throws ParseException {
        try {
            logger.info("Updating booking with ID {} using map data", id);
            BookingRequest updateRequest = createBookingRequestFromMap(bookingData);
            this.response = updateBooking(id, updateRequest, token);
        } catch (Exception e) {
            logger.error("Failed to update booking with map data: {}", e.getMessage());
            throw e;
        }
    }

    private BookingRequest createBookingRequestFromMap(Map<String, String> bookingData) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        Date checkin = sdf.parse(bookingData.get("checkin"));
        Date checkout = sdf.parse(bookingData.get("checkout"));

        return new BookingRequest(
                bookingData.get("firstname"),
                bookingData.get("lastname"),
                Integer.parseInt(bookingData.get("totalprice")),
                Boolean.parseBoolean(bookingData.get("depositpaid")),
                new BookingDates(checkin, checkout),
                bookingData.get("additionalneeds")
        );
    }


    private Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setLenient(false);
        return sdf.parse(dateStr);
    }

    private int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    public void partialUpdateCreatedBooking(DataTable dataTable, String token) {
        try {
            logger.info("Partially updating created booking");
            Map<String, Object> updatePayload = createUpdatePayloadFromDataTable(dataTable);
            logger.debug("Partial update payload: {}", updatePayload);
            partialUpdateBooking(bookingId, updatePayload, token);
            logger.info("Created booking partially updated successfully");
        } catch (Exception e) {
            logger.error("Failed to partially update created booking: {}", e.getMessage());
            throw new RuntimeException("Failed to partially update created booking", e);
        }
    }

    private Map<String, Object> createUpdatePayloadFromDataTable(DataTable dataTable) {
        return dataTable.asMaps().stream()
                .collect(HashMap::new,
                        (map, row) -> map.put(row.get("field"), row.get("value")),
                        HashMap::putAll);
    }

    public void createTestBooking(DataTable dataTable) throws ParseException {
        try {
            logger.info("Creating test booking from DataTable");
            this.currentBookingRequest = createBookingRequestFromDataTable(dataTable);
            createBooking();
            scenarioContext.setContext("test_booking_id", getBookingId());
            logger.info("Test booking created successfully");
        } catch (ParseException e) {
            handleDateParseError(e);
        } catch (NumberFormatException e) {
            handleNumberFormatError(e);
        } catch (Exception e) {
            handleTestBookingCreationError(e);
        }
    }

    private BookingRequest createBookingRequestFromDataTable(DataTable dataTable) throws ParseException {
        Map<String, String> testBookingData = dataTable.asMaps().get(0);
        BookingDates dates = new BookingDates(
                parseDate(testBookingData.get("checkin")),
                parseDate(testBookingData.get("checkout"))
        );

        return new BookingRequest(
                testBookingData.get("firstname"),
                testBookingData.get("lastname"),
                parseInt(testBookingData.getOrDefault("totalprice", "100")),
                parseBoolean(testBookingData.getOrDefault("depositpaid", "true")),
                dates,
                testBookingData.getOrDefault("additionalneeds", "Breakfast")
        );
    }

    private void handleDateParseError(ParseException e) throws ParseException {
        logger.error("Date parsing failed while creating test booking: {}", e.getMessage());
        throw e;
    }

    private void handleNumberFormatError(NumberFormatException e) {
        logger.error("Number format exception while creating test booking: {}", e.getMessage());
        throw new RuntimeException("Invalid number format in booking data", e);
    }

    private void handleTestBookingCreationError(Exception e) {
        logger.error("Failed to create test booking: {}", e.getMessage());
        throw new RuntimeException("Test booking creation failed", e);
    }

    public void prepareBookingDataFromDataTable(DataTable dataTable) throws ParseException {
        Map<String, String> bookingData = dataTable.asMaps().get(0);
        this.currentBookingRequest = createBookingRequestFromMap(bookingData);
    }


    public void verifyBookingIsCreated() {
        try {
            logger.info("Verifying booking creation");
            verifyStatusCode(200);
            extractAndVerifyBookingId();
            logger.info("Booking creation verified successfully with ID: {}", bookingId);
        } catch (Exception e) {
            logger.error("Failed to verify booking creation: {}", e.getMessage());
            throw e;
        }
    }

    private void extractAndVerifyBookingId() {
        this.bookingId = response.jsonPath().getInt("bookingid");
        Assert.assertTrue("Booking ID should be greater than 0", bookingId > 0);
    }

    public void verifyBookingDetailsMatch(Map<String, String> expectedData) {
        try {
            logger.info("Verifying booking details match expected data");
            verifyPersonalDetails(expectedData);
            verifyBookingDates(expectedData);
            logger.info("Booking details verification successful");
        } catch (Exception e) {
            logger.error("Booking details verification failed: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyPersonalDetails(Map<String, String> expectedData) {
        Assert.assertEquals(expectedData.get("firstname"), response.jsonPath().getString("firstname"));
        Assert.assertEquals(expectedData.get("lastname"), response.jsonPath().getString("lastname"));
    }

    private void verifyBookingDates(Map<String, String> expectedData) {
        Assert.assertEquals(expectedData.get("checkin"), response.jsonPath().getString("bookingdates.checkin"));
        Assert.assertEquals(expectedData.get("checkout"), response.jsonPath().getString("bookingdates.checkout"));
    }

    public void verifyBookingDatesValid() {
        try {
            logger.info("Verifying booking dates validity");
            String checkin = getDateFromResponse("checkin");
            String checkout = getDateFromResponse("checkout");
            verifyDateFormats(checkin, checkout);
            verifyDateSequence(checkin, checkout);
            logger.info("Booking dates verified successfully");
        } catch (Exception e) {
            logger.error("Failed to verify booking dates: {}", e.getMessage());
            throw new RuntimeException("Failed to verify booking dates", e);
        }
    }

    private String getDateFromResponse(String dateType) {
        return response.jsonPath().getString("bookingdates." + dateType);
    }

    private void verifyDateFormats(String checkin, String checkout) {
        Assert.assertNotNull(checkin);
        Assert.assertNotNull(checkout);
        Assert.assertTrue(checkin.matches("\\d{4}-\\d{2}-\\d{2}"));
        Assert.assertTrue(checkout.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    private void verifyDateSequence(String checkin, String checkout) throws ParseException {
        Assert.assertTrue(DATE_FORMAT.parse(checkin).before(DATE_FORMAT.parse(checkout)));
    }

    public void verifyBookingDeleted() {
        try {
            logger.info("Verifying booking deletion");
            verifyStatusCode(201);
            verifyBookingDoesNotExist();
            logger.info("Booking deletion verified successfully");
        } catch (Exception e) {
            logger.error("Failed to verify booking deletion: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyBookingDoesNotExist() {
        Assert.assertFalse(checkBookingExists(bookingId));
    }

    public void updateBookingByIdTypeWithData(String idType, DataTable dataTable, String token) throws ParseException {
        int id = resolveBookingId(idType);
        Map<String, String> bookingData = dataTable.asMaps().get(0);
        BookingRequest updateRequest = createBookingRequestFromMap(bookingData);
        updateBooking(id, updateRequest, token);
    }

    public void verifyUpdatedBookingContains(Map<String, String> expectedFields) {
        int attempts = 0;
        AssertionError lastError = null;

        logger.info("Verifying updated booking fields");

        while (attempts < MAX_VERIFICATION_ATTEMPTS) {
            attempts++;
            try {
                Response response = getBookingById(bookingId);
                verifyAllFieldsMatch(expectedFields, response);
                logger.info("Booking fields verified successfully after {} attempts", attempts);
                return;
            } catch (AssertionError e) {
                lastError = e;
                handleVerificationRetry(attempts, e);
            }
        }

        logger.error("Field verification failed after maximum attempts");
        throw lastError;
    }

    private void verifyAllFieldsMatch(Map<String, String> expectedFields, Response response) {
        expectedFields.forEach((field, expectedValue) -> {
            String actualValue = getFieldValueFromResponse(response, field);
            Assert.assertEquals(field + " mismatch", expectedValue, actualValue);
        });
    }

    private String getFieldValueFromResponse(Response response, String field) {
        String jsonPath = isDateField(field) ? "bookingdates." + field : field;
        return response.jsonPath().getString(jsonPath);
    }

    private boolean isDateField(String field) {
        return field.equals("checkin") || field.equals("checkout");
    }

    private void handleVerificationRetry(int attempt, AssertionError error) {
        if (attempt < MAX_VERIFICATION_ATTEMPTS) {
            logger.warn("Field verification failed on attempt {}/{} - retrying...",
                    attempt, MAX_VERIFICATION_ATTEMPTS);
            sleepWithInterruptHandling(VERIFICATION_RETRY_DELAY_MS);
        }
    }

    public void verifyCompleteUpdate(BookingRequest expectedRequest) {
        int attempts = 0;

        logger.info("Starting complete booking update verification");

        while (attempts < MAX_VERIFICATION_ATTEMPTS) {
            attempts++;
            try {
                Response response = getBookingById(bookingId);
                verifyAllBookingFields(expectedRequest, response);
                logger.info("Booking update successfully verified after {} attempts", attempts);
                return;
            } catch (AssertionError e) {
                handleVerificationFailure(attempts, e);
            } catch (Exception e) {
                handleVerificationException(e);
            }
        }
    }

    private void verifyAllBookingFields(BookingRequest expected, Response actualResponse) throws ParseException {
        verifyPersonalInfoFields(expected, actualResponse);
        verifyDateFields(expected, actualResponse);
        verifyPaymentFields(expected, actualResponse);
        verifyAdditionalNeedsField(expected, actualResponse);
    }

    private void verifyPersonalInfoFields(BookingRequest expected, Response actualResponse) {
        verifyFieldEquals("firstname", expected.getFirstname(), actualResponse);
        verifyFieldEquals("lastname", expected.getLastname(), actualResponse);
    }

    private void verifyDateFields(BookingRequest expected, Response actualResponse) throws ParseException {
        verifyDateField("checkin", expected.getBookingdates().getCheckin(), actualResponse);
        verifyDateField("checkout", expected.getBookingdates().getCheckout(), actualResponse);
    }

    private void verifyPaymentFields(BookingRequest expected, Response actualResponse) {
        verifyFieldEquals("totalprice", expected.getTotalprice(), actualResponse);
        verifyFieldEquals("depositpaid", expected.isDepositpaid(), actualResponse);
    }

    private void verifyAdditionalNeedsField(BookingRequest expected, Response actualResponse) {
        verifyFieldEquals("additionalneeds", expected.getAdditionalneeds(), actualResponse);
    }

    private void verifyFieldEquals(String field, Object expected, Object actual) {
        Assert.assertEquals(field + " mismatch", expected, actual);
    }

    private void verifyDateField(String fieldName, Date expectedDate, Response response) throws ParseException {
        String dateString = response.jsonPath().getString("bookingdates." + fieldName);
        Date actualDate = parseDate(dateString);
        long diffMs = Math.abs(expectedDate.getTime() - actualDate.getTime());

        Assert.assertTrue(fieldName + " date difference exceeds tolerance (" + diffMs + "ms)",
                diffMs <= TimeUnit.DAYS.toMillis(1));
    }

    private void handleVerificationFailure(int attempt, AssertionError error) {
        if (attempt >= MAX_VERIFICATION_ATTEMPTS) {
            logger.error("Verification failed after {} attempts: {}", MAX_VERIFICATION_ATTEMPTS, error.getMessage());
            throw error;
        }

        logger.warn("Attempt {}/{} failed: {} - Retrying...",
                attempt, MAX_VERIFICATION_ATTEMPTS, error.getMessage());
        sleepWithInterruptHandling(VERIFICATION_RETRY_DELAY_MS);
    }

    private void handleVerificationException(Exception e) {
        logger.error("Unexpected error during booking verification: {}", e.getMessage());
        throw new RuntimeException("Booking verification failed", e);
    }

    private void sleepWithInterruptHandling(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Verification process interrupted: {}", ie.getMessage());
            throw new RuntimeException("Verification interrupted", ie);
        }
    }

    public void verifyCompleteUpdateFromDataTable(DataTable dataTable) throws ParseException {
        try {
            logger.info("Verifying complete update from DataTable");
            BookingRequest expectedRequest = createExpectedRequestFromDataTable(dataTable);
            verifyCompleteUpdate(expectedRequest);
            logger.info("Complete update from DataTable verified successfully");
        } catch (Exception e) {
            logger.error("Failed to verify complete update from DataTable: {}", e.getMessage());
            throw e;
        }
    }

    private BookingRequest createExpectedRequestFromDataTable(DataTable dataTable) throws ParseException {
        Map<String, String> bookingData = dataTable.asMaps().get(0);
        BookingDates dates = new BookingDates(
                parseDate(bookingData.get("checkin")),
                parseDate(bookingData.get("checkout"))
        );

        return new BookingRequest(
                bookingData.get("firstname"),
                bookingData.get("lastname"),
                parseInt(bookingData.get("totalprice")),
                parseBoolean(bookingData.get("depositpaid")),
                dates,
                bookingData.get("additionalneeds")
        );
    }

    public void verifyUpdatedBookingFromDataTable(DataTable dataTable) {
        try {
            logger.info("Verifying updated booking from DataTable");
            Map<String, String> expectedFields = createExpectedFieldsFromDataTable(dataTable);
            verifyUpdatedBookingContains(expectedFields);
            logger.info("Updated booking from DataTable verified successfully");
        } catch (Exception e) {
            logger.error("Failed to verify updated booking from DataTable: {}", e.getMessage());
            throw e;
        }
    }

    private Map<String, String> createExpectedFieldsFromDataTable(DataTable dataTable) {
        return dataTable.asMaps().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.get("field"), v.get("value")), HashMap::putAll);
    }

    public void verifyStatusCode(int expectedStatusCode) {
        try {
            logger.info("Verifying status code: {}", expectedStatusCode);
            Assert.assertEquals("Status code verification failed",
                    expectedStatusCode, response.getStatusCode());
            logger.info("Status code verified successfully");
        } catch (Exception e) {
            logger.error("Status code verification failed: {}", e.getMessage());
            throw e;
        }
    }

    public void verifyBookingIdsListNotEmpty() {
        try {
            logger.info("Verifying booking IDs list is not empty");
            List<Map<String, Integer>> bookingIds = getBookingIdsFromResponse();
            Assert.assertNotNull(bookingIds);
            Assert.assertFalse(bookingIds.isEmpty());
            logger.info("Booking IDs list verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify booking IDs list: {}", e.getMessage());
            throw e;
        }
    }

    private List<Map<String, Integer>> getBookingIdsFromResponse() {
        return response.jsonPath().getList("");
    }

    public void verifyResponseContainsCreatedBookingId() {
        try {
            logger.info("Verifying response contains created booking ID");
            boolean found = checkIfBookingIdExistsInResponse();
            Assert.assertTrue("Response should contain created booking ID", found);
            logger.info("Created booking ID verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify created booking ID in response: {}", e.getMessage());
            throw e;
        }
    }

    private boolean checkIfBookingIdExistsInResponse() {
        List<Map<String, Integer>> bookings = response.jsonPath().getList("");
        return bookings.stream()
                .anyMatch(booking -> booking.get("bookingid") == bookingId);
    }

    public void verifyFilteredBookingsMatchName() {
        try {
            logger.info("Verifying filtered bookings match name");
            List<Map<String, Integer>> bookings = getBookingIdsFromResponse();
            verifyEachBookingMatchesNameFilter(bookings);
            logger.info("Filtered bookings name verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify filtered bookings match name: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyEachBookingMatchesNameFilter(List<Map<String, Integer>> bookings) {
        bookings.forEach(booking -> {
            int id = booking.get("bookingid");
            Response bookingResponse = getBookingById(id);
            Assert.assertEquals(
                    scenarioContext.getFilterFirstname(),
                    bookingResponse.jsonPath().getString("firstname")
            );
            Assert.assertEquals(
                    scenarioContext.getFilterLastname(),
                    bookingResponse.jsonPath().getString("lastname")
            );
        });
    }

    public void verifyFilteredBookingsMatchDates() throws Exception {
        try {
            logger.info("Verifying filtered bookings match dates");
            Date filterCheckin = parseDate(scenarioContext.getFilterCheckin());
            Date filterCheckout = parseDate(scenarioContext.getFilterCheckout());

            List<Map<String, Integer>> bookings = getBookingIdsFromResponse();
            verifyEachBookingMatchesDateFilter(bookings, filterCheckin, filterCheckout);
            logger.info("Filtered bookings dates verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify filtered bookings match dates: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyEachBookingMatchesDateFilter(List<Map<String, Integer>> bookings, Date filterCheckin, Date filterCheckout) {
        bookings.forEach(booking -> {
            int id = booking.get("bookingid");
            Response bookingResponse = getBookingById(id);
            String checkinStr = bookingResponse.jsonPath().getString("bookingdates.checkin");
            String checkoutStr = bookingResponse.jsonPath().getString("bookingdates.checkout");

            if (isValidDate(checkinStr) && isValidDate(checkoutStr)) {
                try {
                    verifyDateRange(filterCheckin, filterCheckout, checkinStr, checkoutStr);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse dates for booking ID " + id, e);
                }
            }
        });
    }

    private void verifyDateRange(Date filterCheckin, Date filterCheckout, String checkinStr, String checkoutStr) throws ParseException {
        Date checkin = parseDate(checkinStr);
        Date checkout = parseDate(checkoutStr);
        Assert.assertFalse("Checkin date should not be before filter", checkin.before(filterCheckin));
        Assert.assertFalse("Checkout date should not be after filter", checkout.after(filterCheckout));
    }

    public void verifyResponseIsEmptyArray() {
        try {
            logger.info("Verifying response is empty array");
            List<Map<String, Integer>> bookings = getBookingIdsFromResponse();
            Assert.assertTrue(bookings.isEmpty());
            logger.info("Empty array verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify empty array response: {}", e.getMessage());
            throw e;
        }
    }

    public void verifyValidBookingDetails() {
        try {
            logger.info("Verifying valid booking details");
            verifyRequiredFieldsExist();
            logger.info("Valid booking details verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify valid booking details: {}", e.getMessage());
            throw e;
        }
    }

    private void verifyRequiredFieldsExist() {
        Assert.assertNotNull(response.jsonPath().getString("firstname"));
        Assert.assertNotNull(response.jsonPath().getString("lastname"));
        Assert.assertNotNull(response.jsonPath().getMap("bookingdates"));
    }

    public void verifyBookingIdInResponse() {
        try {
            logger.info("Verifying booking ID in response");
            extractAndVerifyBookingId();
            logger.info("Booking ID in response verification successful");
        } catch (Exception e) {
            logger.error("Failed to verify booking ID in response: {}", e.getMessage());
            throw e;
        }
    }

    public void verifyResponseMatchesRequest() {
        BookingResponse bookingResponse = response.as(BookingResponse.class);
        BookingRequest createdBooking = bookingResponse.getBooking();

        verifyDatesMatch(
                currentBookingRequest.getBookingdates().getCheckin(),
                createdBooking.getBookingdates().getCheckin(),
                currentBookingRequest.getBookingdates().getCheckout(),
                createdBooking.getBookingdates().getCheckout()
        );

        verifyFieldEquals("firstname", currentBookingRequest.getFirstname(), createdBooking.getFirstname());
        verifyFieldEquals("lastname", currentBookingRequest.getLastname(), createdBooking.getLastname());
        verifyFieldEquals("totalprice", currentBookingRequest.getTotalprice(), createdBooking.getTotalprice());
        verifyFieldEquals("depositpaid", currentBookingRequest.isDepositpaid(), createdBooking.isDepositpaid());
        verifyFieldEquals("additionalneeds", currentBookingRequest.getAdditionalneeds(), createdBooking.getAdditionalneeds());
    }

    private void verifyPersonalInfoMatch(BookingRequest expected, BookingRequest actual) {
        verifyFieldMatch("firstname", expected.getFirstname(), actual.getFirstname());
        verifyFieldMatch("lastname", expected.getLastname(), actual.getLastname());
    }

    private void verifyDatesMatch(Date expectedCheckin, Date actualCheckin,
                                  Date expectedCheckout, Date actualCheckout) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        Assert.assertEquals("checkin date mismatch",
                sdf.format(expectedCheckin),
                sdf.format(actualCheckin));

        Assert.assertEquals("checkout date mismatch",
                sdf.format(expectedCheckout),
                sdf.format(actualCheckout));
    }

    private void verifyPaymentInfoMatch(BookingRequest expected, BookingRequest actual) {
        verifyFieldMatch("totalprice", expected.getTotalprice(), actual.getTotalprice());
        verifyFieldMatch("depositpaid", expected.isDepositpaid(), actual.isDepositpaid());
    }

    private void verifyAdditionalNeedsMatch(BookingRequest expected, BookingRequest actual) {
        verifyFieldMatch("additionalneeds", expected.getAdditionalneeds(), actual.getAdditionalneeds());
    }

    private void verifyFieldMatch(String fieldName, Object expected, Object actual) {
        Assert.assertEquals(
                String.format("%s mismatch (expected: %s, actual: %s)", fieldName, expected, actual),
                expected,
                actual
        );
    }

    private void verifyDateMatch(String dateType, Date expectedDate, Date actualDate) {
        long tolerance = TimeUnit.HOURS.toMillis(12); // 12 saat tolerans
        long diff = Math.abs(expectedDate.getTime() - actualDate.getTime());

        if (diff > tolerance) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String expectedStr = sdf.format(expectedDate);
            String actualStr = sdf.format(actualDate);
            Assert.fail(dateType + " date mismatch (expected: " + expectedStr + ", actual: " + actualStr + ")");
        }
    }

    public void createCurrentBooking() {
        try {
            logger.info("Creating current booking");
            this.response = executeBookingPostRequest(currentBookingRequest);
            this.bookingId = extractBookingIdFromBookingResponse();
            logger.info("Successfully created booking with ID: {}", bookingId);
        } catch (RuntimeException e) {
            handleBookingCreationError(e);
        } catch (Exception e) {
            handleUnexpectedBookingCreationError(e);
        }
    }

    private int extractBookingIdFromBookingResponse() {
        return response.as(BookingResponse.class).getBookingid();
    }

    private void handleBookingCreationError(RuntimeException e) {
        logger.error("Booking creation failed: {}", e.getMessage());
        throw e;
    }

    private void handleUnexpectedBookingCreationError(Exception e) {
        logger.error("Unexpected error creating booking: {}", e.getMessage());
        throw new RuntimeException("Booking creation failed", e);
    }

    private Response executeBookingPostRequest(Object body) {
        try {
            logger.debug("Executing booking creation request");
            Response response = RestAssured.given()
                    .baseUri(TestConfig.getBaseUrl())
                    .contentType(CONTENT_TYPE)
                    .body(body)
                    .post(BOOKING_ENDPOINT);
            logResponseDetails(response);
            return response;
        } catch (Exception e) {
            logger.error("Booking creation request failed: {}", e.getMessage());
            throw new RuntimeException("Booking creation request failed", e);
        }
    }

    public int resolveBookingId(String idType) {
        try {
            logger.debug("Resolving booking ID for type: {}", idType);
            return idType.equals("created") ? getBookingId() : Integer.parseInt(idType);
        } catch (Exception e) {
            logger.error("Failed to resolve booking ID: {}", e.getMessage());
            throw new RuntimeException("Failed to resolve booking ID", e);
        }
    }

    public int resolveBookingIdForDeletion(String idType) {
        try {
            logger.debug("Resolving booking ID for deletion: {}", idType);
            if (idType.equals("created")) {
                verifyBookingExistsForDeletion();
                return bookingId;
            }
            return Integer.parseInt(idType);
        } catch (Exception e) {
            logger.error("Failed to resolve booking ID for deletion: {}", e.getMessage());
            throw new RuntimeException("Failed to resolve booking ID for deletion", e);
        }
    }

    private void verifyBookingExistsForDeletion() {
        if (bookingId == 0) {
            throw new IllegalStateException("No booking has been created yet");
        }
    }

    public void deleteBookingByIdType(String idType, String token) {
        try {
            logger.info("Attempting to delete booking with ID type: {}", idType);
            int id = resolveBookingIdForDeletion(idType);
            this.response = deleteBooking(id, token);
            logger.info("Successfully deleted booking with ID type: {}", idType);
        } catch (IllegalStateException e) {
            handleInvalidBookingStateError(idType, e);
        } catch (IllegalArgumentException e) {
            handleInvalidBookingIdError(idType, e);
        } catch (Exception e) {
            handleUnexpectedDeletionError(idType, e);
        }
    }

    private void handleInvalidBookingStateError(String idType, IllegalStateException e) {
        logger.error("Invalid booking state for deletion - ID type: {} - Error: {}", idType, e.getMessage());
        throw e;
    }

    private void handleInvalidBookingIdError(String idType, IllegalArgumentException e) {
        logger.error("Invalid booking ID format - Type: {} - Error: {}", idType, e.getMessage());
        throw e;
    }

    private void handleUnexpectedDeletionError(String idType, Exception e) {
        logger.error("Unexpected error deleting booking - ID type: {} - Error: {}", idType, e.getMessage());
        throw new RuntimeException("Booking deletion failed", e);
    }

    private boolean checkBookingExists(int bookingId) {
        try {
            logger.debug("Checking if booking exists with ID: {}", bookingId);
            Response response = executeGetRequest(buildBookingPath(bookingId));
            return response.getStatusCode() == 200;
        } catch (Exception e) {
            logger.error("Failed to check booking existence: {}", e.getMessage());
            return false;
        }
    }

    private Response executeGetRequest(String path) {
        try {
            logger.debug("Executing GET request to: {}", path);
            Response response = RestAssured.given()
                    .baseUri(TestConfig.getBaseUrl())
                    .get(path);
            logResponseDetails(response);
            return response;
        } catch (Exception e) {
            logger.error("GET request failed to {}: {}", path, e.getMessage());
            throw new RuntimeException("GET request failed", e);
        }
    }

    public void createBasicBooking(String firstname, String lastname, String checkin, String checkout) throws ParseException {
        BookingDates dates = createBookingDates(checkin, checkout);
        this.currentBookingRequest = createDefaultBookingRequest(firstname, lastname, dates);
        createBooking();
    }

    private BookingDates createBookingDates(String checkin, String checkout) throws ParseException {
        return new BookingDates(
                parseDate(checkin),
                parseDate(checkout)
        );
    }

    private BookingRequest createDefaultBookingRequest(String firstname, String lastname, BookingDates dates) {
        return new BookingRequest(
                firstname,
                lastname,
                100,
                true,
                dates,
                "Breakfast"
        );
    }

    private String buildBookingPath(int id) {
        return BOOKING_ENDPOINT + "/" + id;
    }

    private void logResponseDetails(Response response) {
        logger.info("Response Status Code: {}", response.getStatusCode());
        logger.debug("Response Headers: {}", response.getHeaders());
        logger.trace("Response Body: {}", response.getBody().asString());
    }

    private boolean isValidDate(String dateStr) {
        return dateStr != null && dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    public int getBookingId() {
        return this.bookingId;
    }

    public BookingRequest getCurrentBookingRequest() {
        return this.currentBookingRequest;
    }
}