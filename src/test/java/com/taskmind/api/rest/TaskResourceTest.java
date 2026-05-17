package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskResourceTest {

    @Inject TestDataCleanup cleanup;

    private static Integer projectId;
    private static Integer taskId;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("taskuser-" + System.currentTimeMillis(), "task@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void shouldCreateProjectForTasks() {
        Response response = authenticated()
            .body("{\"name\": \"stage2-test-project\", \"description\": \"Project for task testing\"}")
        .when().post("/api/projects")
        .then()
            .statusCode(201)
        .extract().response();

        projectId = response.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldCreateTask() {
        Response response = authenticated()
            .body("{\"title\": \"Implement RAG Search\", \"description\": \"Add semantic search for tasks\", \"tags\": [\"ai\", \"backend\"]}")
        .when()
            .post("/api/projects/{id}/tasks", projectId)
        .then()
            .statusCode(201)
            .body("title", equalTo("Implement RAG Search"))
            .body("description", equalTo("Add semantic search for tasks"))
            .body("id", notNullValue())
            .body("projectId", equalTo(projectId))
            .body("tags", hasItems("ai", "backend"))
            .body("isArchived", equalTo(false))
        .extract().response();

        taskId = response.jsonPath().getInt("id");
    }

    @Test
    @Order(3)
    void shouldListTasks() {
        authenticated()
            .when().get("/api/projects/{id}/tasks", projectId)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("title", hasItem("Implement RAG Search"));
    }

    @Test
    @Order(4)
    void shouldDeleteTask() {
        authenticated()
            .when().delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
            .then()
            .statusCode(204);

        authenticated()
            .when().get("/api/projects/{id}/tasks", projectId)
            .then()
            .body("title", not(hasItem("Implement RAG Search")));
    }

    @Test
    @Order(5)
    void shouldRejectUnauthenticatedAccess() {
        given()
            .contentType(io.restassured.http.ContentType.JSON)
            .body("{\"title\": \"Unauth Task\"}")
        .when()
            .post("/api/projects/{id}/tasks", 999)
        .then()
            .statusCode(401);
    }

    @Test
    @Order(6)
    void testCreateTaskWithLargeDescription() {
        // Adding a test case for large descriptions, similar to what might be in GlobalTask tests
        String largeDesc = "A".repeat(1000);
        authenticated()
            .body("{\"title\": \"Large Task\", \"description\": \"" + largeDesc + "\"}")
        .when()
            .post("/api/projects/{id}/tasks", projectId)
        .then()
            .statusCode(201)
            .body("description", hasLength(1000));
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
