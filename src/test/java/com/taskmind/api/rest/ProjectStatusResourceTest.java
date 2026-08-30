package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ProjectStatusResourceTest {

    @Inject TestDataCleanup cleanup;

    /** projectId=1 и его статусы приходят из сида — восстанавливаем перед каждым методом. */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void testProjectStatuses() {
        String token = TestAuthHelper.registerAndLogin("status_reader", "status_reader@test.com", "password");

        // Path is /api/project-statuses/project/{projectId}
        TestAuthHelper.authenticated(token)
            .get("/api/project-statuses/project/1")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)));
    }

    @Test
    public void testCreateStatus() {
        String token = TestAuthHelper.registerAndLogin("status_creator", "status_creator@test.com", "password");

        String body = "{\"projectId\":1, \"statusName\":\"DONE\", \"statusColor\":\"#00FF00\", \"statusOrder\":1, \"isDefault\":false, \"isClosed\":true}";

        TestAuthHelper.authenticated(token)
            .contentType(ContentType.JSON)
            .body(body)
            .post("/api/project-statuses")
            .then()
            .statusCode(201);
    }
}
