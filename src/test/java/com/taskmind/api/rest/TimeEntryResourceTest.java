package com.taskmind.api.rest;

import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.TimeEntry;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TimeEntryResourceTest {

    @Test
    public void shouldLogTimeAndGetTotalTime() {
        String token = TestAuthHelper.registerAndLogin("user", "user@test.com", "password");
        
        // Assume task with ID 1 exists
        Map<String, Object> logBody = Map.of(
            "userId", 1,
            "seconds", 3600,
            "description", "Test work"
        );

        TestAuthHelper.authenticated(token)
            .body(logBody)
            .post("/api/tasks/1/time")
            .then()
            .statusCode(200);

        TestAuthHelper.authenticated(token)
            .get("/api/tasks/1/time")
            .then()
            .statusCode(200)
            .body(is("3600"));
    }
}
