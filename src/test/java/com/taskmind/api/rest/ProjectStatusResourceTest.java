package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ProjectStatusResourceTest {

    @Test
    public void testProjectStatuses() {
        String token = TestAuthHelper.registerAndLogin("admin_tester", "admin@test.com", "password");

        // Path is /api/project-statuses/project/{projectId}
        TestAuthHelper.authenticated(token)
            .get("/api/project-statuses/project/1")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)));
    }

    @Test
    public void testCreateStatus() {
        String token = TestAuthHelper.registerAndLogin("admin_creator", "creator@test.com", "password");

        String body = "{\"projectId\":1, \"statusName\":\"DONE\", \"statusColor\":\"#00FF00\", \"statusOrder\":1, \"isDefault\":false, \"isClosed\":true}";

        TestAuthHelper.authenticated(token)
            .contentType(ContentType.JSON)
            .body(body)
            .post("/api/project-statuses")
            .then()
            .statusCode(201);
    }
}
