package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceTest {

    @Inject TestDataCleanup cleanup;

    private static String token;
    private static final String TEST_USER = "authuser-" + System.currentTimeMillis();
    private static final String TEST_EMAIL = TEST_USER + "@example.com";
    private static final String TEST_PASS = "StrongPass123!";
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            initialized = true;
        }
    }

    @Test @Order(1)
    void shouldRegisterAndGetToken() {
        String body = "{\"username\":\"" + TEST_USER + "\",\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASS + "\"}";
        Response response = given().contentType(ContentType.JSON)
            .body(body)
        .when().post("/api/auth/register");

        token = response.jsonPath().getString("token");
        Assertions.assertNotNull(token, "Token should not be null. Response: " + response.getBody().asString());
    }

    @Test @Order(2)
    void shouldLoginAndGetToken() {
        given().contentType(ContentType.JSON)
            .body("{\"username\":\"" + TEST_USER + "\",\"password\":\"" + TEST_PASS + "\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo(TEST_USER));
    }

    @Test @Order(3)
    void shouldAccessProtectedEndpointWithToken() {
        Assertions.assertNotNull(token, "Token must be set");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/users/me")
        .then().statusCode(200)
            .body("username", equalTo(TEST_USER))
            .body("email", equalTo(TEST_EMAIL));
    }

    @Test @Order(4)
    void shouldRejectInvalidCredentials() {
        given().contentType(ContentType.JSON)
            .body("{\"username\":\"" + TEST_USER + "\",\"password\":\"WrongPass\"}")
        .when().post("/api/auth/login")
        .then().statusCode(401);
    }

    /** Занятый логин — 409. Раньше тут закреплялась 500: по ней форма регистрации
     * не отличит «логин занят» от настоящей поломки сервера. */
    @Test @Order(5)
    void shouldRejectDuplicateUsername() {
        given().contentType(ContentType.JSON)
            .body("{\"username\":\"" + TEST_USER + "\",\"email\":\"other@example.com\",\"password\":\"" + TEST_PASS + "\"}")
        .when().post("/api/auth/register")
        .then().statusCode(409);
    }

    /** Занятый email проверялся в коде, но не был покрыт тестом вовсе. */
    @Test @Order(6)
    void shouldRejectDuplicateEmail() {
        given().contentType(ContentType.JSON)
            .body("{\"username\":\"other-" + TEST_USER + "\",\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASS + "\"}")
        .when().post("/api/auth/register")
        .then().statusCode(409);
    }

    @Test @Order(7)
    void shouldRejectUnauthenticatedAccess() {
        given()
        .when().get("/api/users/me")
        .then().statusCode(401);
    }
}
