package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TimeEntryResourceTest {

    @Inject TestDataCleanup cleanup;

    /** Тест проверяет точные суммы по задаче 1, поэтому ему нужна пустая таблица timeEntries. */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void shouldLogTimeAndGetTotalTime() {
        String token = TestAuthHelper.registerAndLogin("time_user", "time_user@test.com", "password");

        // Задача с ID 1 приходит из сида
        TestAuthHelper.authenticated(token)
            .body("{\"seconds\": 3600, \"description\": \"Test work\"}")
            .post("/api/tasks/1/time")
            .then()
            .statusCode(200)
            .body("seconds", is(3600));

        TestAuthHelper.authenticated(token)
            .get("/api/tasks/1/time")
            .then()
            .statusCode(200)
            .body(is("3600"));
    }

    @Test
    public void shouldAttributeEntryToCallerNotToUserIdFromBody() {
        String token = TestAuthHelper.registerAndLogin("time_owner", "time_owner@test.com", "password");
        int callerId = TestAuthHelper.authenticated(token)
            .get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");

        // Тело намеренно пытается подставить чужого автора — userId=1 (admin из сида).
        TestAuthHelper.authenticated(token)
            .body("{\"userId\": 1, \"seconds\": 1200, \"description\": \"Чужое время\"}")
            .post("/api/tasks/1/time")
            .then()
            .statusCode(200)
            .body("userId", equalTo(callerId))
            .body("userId", not(equalTo(1)));
    }

    @Test
    public void shouldKeepEntriesOfDifferentUsersSeparateButSumThemTogether() {
        String tokenA = TestAuthHelper.registerAndLogin("time_a", "time_a@test.com", "password");
        String tokenB = TestAuthHelper.registerAndLogin("time_b", "time_b@test.com", "password");

        int userA = TestAuthHelper.authenticated(tokenA).get("/api/users/me")
            .then().statusCode(200).extract().jsonPath().getInt("id");
        int userB = TestAuthHelper.authenticated(tokenB).get("/api/users/me")
            .then().statusCode(200).extract().jsonPath().getInt("id");

        TestAuthHelper.authenticated(tokenA)
            .body("{\"seconds\": 1800, \"description\": \"A\"}")
            .post("/api/tasks/1/time")
            .then().statusCode(200).body("userId", equalTo(userA));

        TestAuthHelper.authenticated(tokenB)
            .body("{\"seconds\": 1800, \"description\": \"B\"}")
            .post("/api/tasks/1/time")
            .then().statusCode(200).body("userId", equalTo(userB));

        TestAuthHelper.authenticated(tokenA)
            .get("/api/tasks/1/time")
            .then().statusCode(200).body(is("3600"));
    }

    @Test
    public void shouldRejectRequestWithoutSeconds() {
        String token = TestAuthHelper.registerAndLogin("time_invalid", "time_invalid@test.com", "password");

        TestAuthHelper.authenticated(token)
            .body("{\"description\": \"без часов\"}")
            .post("/api/tasks/1/time")
            .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectUnauthenticated() {
        given().contentType(ContentType.JSON)
            .body("{\"seconds\": 600}")
            .post("/api/tasks/1/time")
            .then()
            .statusCode(401);
    }
}
