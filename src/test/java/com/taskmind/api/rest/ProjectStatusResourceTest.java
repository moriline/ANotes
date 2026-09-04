package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * {@code /api/project-statuses}: читать и заводить статусы можно только в проекте,
 * к которому у вызывающего есть доступ.
 */
@QuarkusTest
public class ProjectStatusResourceTest {

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private Integer projectId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("status-owner-" + ts, "status-owner-" + ts + "@test.com", "Pass123!");
        projectId = auth(ownerToken).body("{\"name\": \"status-proj-" + ts + "\"}")
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");
    }

    @Test
    public void memberListsAndCreatesStatuses() {
        // проект приезжает с дефолтной доской из трёх колонок
        auth(ownerToken)
            .get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).body("$", hasSize(3));

        String body = "{\"projectId\":" + projectId + ", \"statusName\":\"DONE\", \"statusColor\":\"#00FF00\","
            + " \"statusOrder\":9, \"isDefault\":false, \"isClosed\":true}";
        auth(ownerToken).body(body)
            .post("/api/project-statuses")
            .then().statusCode(201);

        auth(ownerToken)
            .get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).body("$", hasSize(4));
    }

    @Test
    public void strangerCannotListOrCreateStatusesInAForeignProject() {
        long ts = System.nanoTime();
        String strangerToken = TestAuthHelper.registerAndLogin("status-stranger-" + ts, "status-stranger-" + ts + "@test.com", "Pass123!");

        auth(strangerToken)
            .get("/api/project-statuses/project/" + projectId)
            .then().statusCode(403);

        String body = "{\"projectId\":" + projectId + ", \"statusName\":\"X\", \"statusColor\":\"#000000\","
            + " \"statusOrder\":1, \"isDefault\":false, \"isClosed\":false}";
        auth(strangerToken).body(body)
            .post("/api/project-statuses")
            .then().statusCode(403);
    }

    @Test
    public void unknownProjectIs404() {
        auth(ownerToken)
            .get("/api/project-statuses/project/999999")
            .then().statusCode(404);
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
