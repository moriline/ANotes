package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskResourceTest {

    @Inject TestDataCleanup cleanup;

    private static String projectId;
    private static String taskId;
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
            .body("{\"name\": \"stage2-test-project\"}")
        .when().post("/api/projects")
        .then()
            .statusCode(201)
        .extract().response();

        projectId = response.jsonPath().getString("id");
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
            .body("status", equalTo("TODO"))
            .body("id", notNullValue())
            .body("projectId", equalTo(projectId))
        .extract().response();

        taskId = response.jsonPath().getString("id");
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
    void shouldSearchTasks() {
        authenticated()
            .when().get("/api/projects/{id}/tasks/search?q=semantic", projectId)
            .then()
            .statusCode(200);
    }

    @Test
    @Order(5)
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
    @Order(6)
    void shouldCleanupProject() {
        if (projectId != null) {
            authenticated()
                .when().delete("/api/projects/{id}", projectId)
                .then()
                .statusCode(204);
        }
    }

    @Test
    @Order(7)
    void shouldRejectUnauthenticatedAccess() {
        given()
            .contentType(io.restassured.http.ContentType.JSON)
            .body("{\"title\": \"Unauth Task\"}")
        .when()
            .post("/api/projects/{id}/tasks", "00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(401);
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
