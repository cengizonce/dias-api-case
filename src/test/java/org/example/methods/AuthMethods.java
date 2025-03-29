package org.example.methods;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.TestConfig;
import org.example.models.AuthRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.AuthResponse;
import org.junit.Assert;

public class AuthMethods {
    private static final Logger logger = LogManager.getLogger(AuthMethods.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CONTENT_TYPE = "application/json";
    private static final String AUTH_ENDPOINT = "/auth";
    private static final String TOKEN_KEY = "token";

    private Response response;
    private String token;
    private String username;
    private String password;

    public void setInvalidCredentials(String username, String password) {
        try {
            logger.info("Attempting to set invalid credentials");
            validateCredentials(username, password);
            this.username = username;
            this.password = password;
            logger.debug("Credentials set - Username: {}, Password: {}", username, maskPassword(password));
            logger.info("Successfully set invalid credentials");
        } catch (IllegalArgumentException e) {
            logger.error("Failed to set invalid credentials: {}", e.getMessage());
            throw e;
        }
    }

    public void loadAdminCredentials() {
        try {
            logger.info("Loading admin credentials");
            this.username = TestConfig.getAdminUsername();
            this.password = TestConfig.getAdminPassword();
            validateCredentials(username, password);
            logger.debug("Admin credentials loaded - Username: {}", username);
            logger.info("Successfully loaded admin credentials");
        } catch (Exception e) {
            logger.error("Failed to load admin credentials: {}", e.getMessage());
            throw new RuntimeException("Failed to load admin credentials", e);
        }
    }

    public void createToken() {
        try {
            logger.info("Creating authentication token");
            validateCurrentCredentials();
            AuthRequest authRequest = buildAuthRequest();
            executeAuthRequest(authRequest);
            logger.info("Successfully created authentication token");
        } catch (Exception e) {
            logger.error("Failed to create token: {}", e.getMessage());
            throw new RuntimeException("Token creation failed", e);
        }
    }

    public void verifyTokenIsGenerated() {
        try {
            logger.info("Verifying token generation");
            this.token = extractTokenFromResponse();
            Assert.assertNotNull("Token should not be null", token);
            Assert.assertFalse("Token should not be empty", token.isEmpty());
            logger.debug("Token verified - Length: {}", token.length());
            logger.info("Successfully verified token generation");
        } catch (Exception e) {
            logger.error("Token verification failed: {}", e.getMessage());
            throw new RuntimeException("Token verification failed", e);
        }
    }

    public void verifyAuthRequestFailsWithStatusCode(int expectedStatusCode) {
        try {
            logger.info("Verifying failed authentication with status code: {}", expectedStatusCode);
            int actualStatusCode = response.getStatusCode();
            Assert.assertEquals(
                    String.format("Expected status code %d but got %d", expectedStatusCode, actualStatusCode),
                    expectedStatusCode,
                    actualStatusCode
            );
            logger.info("Successfully verified failed authentication");
        } catch (AssertionError e) {
            logger.error("Status code verification failed: {}", e.getMessage());
            throw e;
        }
    }

    public String generateValidToken() {
        try {
            logger.info("Generating valid token");
            loadAdminCredentials();
            createToken();
            verifyTokenIsGenerated();
            logger.info("Successfully generated valid token");
            return getToken();
        } catch (Exception e) {
            logger.error("Failed to generate valid token: {}", e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public String getToken() {
        try {
            logger.debug("Retrieving token");
            if (token == null) {
                logger.warn("Attempting to get token before generation");
            }
            return token;
        } catch (Exception e) {
            logger.error("Failed to retrieve token: {}", e.getMessage());
            throw new RuntimeException("Token retrieval failed", e);
        }
    }

    private void validateCurrentCredentials() {
        try {
            logger.debug("Validating current credentials");
            validateCredentials(username, password);
        } catch (Exception e) {
            logger.error("Current credentials validation failed: {}", e.getMessage());
            throw e;
        }
    }

    private AuthRequest buildAuthRequest() {
        try {
            logger.debug("Building auth request");
            return new AuthRequest(username, password);
        } catch (Exception e) {
            logger.error("Failed to build auth request: {}", e.getMessage());
            throw new RuntimeException("Auth request construction failed", e);
        }
    }

    private void validateCredentials(String username, String password) {
        try {
            logger.debug("Validating credentials");
            if (isBlank(username)) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            if (isBlank(password)) {
                throw new IllegalArgumentException("Password cannot be empty");
            }
        } catch (Exception e) {
            logger.error("Credentials validation failed: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isBlank(String value) {
        try {
            return value == null || value.trim().isEmpty();
        } catch (Exception e) {
            logger.error("Blank check failed: {}", e.getMessage());
            throw new RuntimeException("Blank check operation failed", e);
        }
    }

    private void executeAuthRequest(AuthRequest authRequest) {
        try {
            logger.debug("Executing auth request");
            String requestBody = serializeAuthRequest(authRequest);
            sendAuthRequest(requestBody);
            logResponseDetails();
        } catch (Exception e) {
            logger.error("Auth request execution failed: {}", e.getMessage());
            throw new RuntimeException("Auth request failed", e);
        }
    }

    private String serializeAuthRequest(AuthRequest authRequest) throws JsonProcessingException {
        try {
            logger.trace("Serializing auth request");
            return mapper.writeValueAsString(authRequest);
        } catch (JsonProcessingException e) {
            logger.error("Auth request serialization failed: {}", e.getMessage());
            throw e;
        }
    }

    private void sendAuthRequest(String requestBody) {
        try {
            logger.debug("Sending auth request to endpoint: {}", AUTH_ENDPOINT);
            this.response = RestAssured.given()
                    .baseUri(TestConfig.getBaseUrl())
                    .contentType(CONTENT_TYPE)
                    .body(requestBody)
                    .post(AUTH_ENDPOINT);
        } catch (Exception e) {
            logger.error("Failed to send auth request: {}", e.getMessage());
            throw new RuntimeException("Request sending failed", e);
        }
    }

    private String extractTokenFromResponse() {
        try {
            logger.debug("Extracting token from response");
            AuthResponse authResponse = response.as(AuthResponse.class);
            if (authResponse.getToken() == null || authResponse.getToken().isEmpty()) {
                throw new RuntimeException("Token not found in response");
            }
            return authResponse.getToken();
        } catch (Exception e) {
            logger.error("Token extraction failed: {}", e.getMessage());
            throw new RuntimeException("Token extraction failed", e);
        }
    }

    private void logResponseDetails() {
        try {
            logger.debug("Logging response details");
            logger.info("Auth response status code: {}", response.getStatusCode());
            logger.debug("Response body: {}", response.getBody().asString());
            logger.trace("Response headers: {}", response.getHeaders());
        } catch (Exception e) {
            logger.error("Failed to log response details: {}", e.getMessage());
        }
    }

    private String maskPassword(String password) {
        try {
            return password == null ? "null" : password.replaceAll(".", "*");
        } catch (Exception e) {
            logger.error("Password masking failed: {}", e.getMessage());
            return "[MASKING_ERROR]";
        }
    }
}