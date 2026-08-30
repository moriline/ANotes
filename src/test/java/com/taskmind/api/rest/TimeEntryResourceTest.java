package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TimeEntryResourceTest {

    @Inject TestDataCleanup cleanup;

    /** Тест проверяет точную сумму по задаче 1, поэтому ему нужна пустая таблица timeEntries. */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void shouldLogTimeAndGetTotalTime() {
        String token = TestAuthHelper.registerAndLogin("time_user", "time_user@test.com", "password");

        // Задача с ID 1 приходит из сида
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
