package org.example.methods;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.TestConfig;
import org.junit.Assert;

public class PingMethods {
    private static final Logger logger = LogManager.getLogger(PingMethods.class);

    public Response healthCheck() {
        Response response = RestAssured.given()
                .baseUri(TestConfig.getBaseUrl())
                .when()
                .get("/ping");

        logger.info("Ping Status Code: {}", response.getStatusCode());
        logger.debug("Ping Response: {}", response.getBody().asString());
        return response;
    }

    public void verifyApiIsRunning(Response response) {
        logger.info("Verifying API health status");
        int actualStatusCode = response.getStatusCode();
        logger.debug("Actual status code: {}, Expected: 201", actualStatusCode);
        Assert.assertEquals("API is not running as expected", 201, actualStatusCode);
    }
}