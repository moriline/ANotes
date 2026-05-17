package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FindResourceTest {

    @Inject TestDataCleanup cleanup;

    private static Integer projectId;
    private static Integer taskId;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("finduser-" + System.currentTimeMillis(), "find@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void prepareData() {
        // Create Project
        Response projResp = authenticated()
            .body("{\"name\": \"find-project\"}")
        .when().post("/api/projects")
        .then().statusCode(201).extract().response();
        projectId = projResp.jsonPath().getInt("id");

        // Create Task
        Response taskResp = authenticated()
            .body("{\"title\": \"Find Me\", \"description\": \"Searching for this task\"}")
        .when().post("/api/projects/" + projectId + "/tasks")
        .then().statusCode(201).extract().response();
        taskId = taskResp.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldFindTaskByTitle() {
        authenticated()
            .contentType(ContentType.JSON)
            .body("{\"titleSearch\": \"Find\"}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks.title", hasItem("Find Me"));
    }

    @Test
    @Order(3)
    void shouldFindTaskByProjectId() {
        authenticated()
            .contentType(ContentType.JSON)
            .body("{\"projectId\": " + projectId + "}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].projectId", equalTo(projectId));
    }

    @Test
    @Order(4)
    void shouldReturnEmptyForNonexistentTitle() {
        authenticated()
            .contentType(ContentType.JSON)
            .body("{\"titleSearch\": \"NoSuchTaskExists\"}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks", hasSize(0));
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
