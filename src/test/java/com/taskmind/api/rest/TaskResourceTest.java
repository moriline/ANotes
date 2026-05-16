package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskResourceTest {

    private static String projectId;
    private static String taskId;

    @Test
    @Order(1)
    void shouldCreateProjectForTasks() {
        Response response = given()
            .contentType(ContentType.JSON)
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
        Response response = given()
            .contentType(ContentType.JSON)
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
        given()
            .when().get("/api/projects/{id}/tasks", projectId)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("title", hasItem("Implement RAG Search"));
    }

    @Test
    @Order(4)
    void shouldSearchTasks() {
        given()
            .when().get("/api/projects/{id}/tasks/search?q=semantic", projectId)
            .then()
            .statusCode(200);
    }

    @Test
    @Order(5)
    void shouldDeleteTask() {
        given()
            .when().delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
            .then()
            .statusCode(204);

        given()
            .when().get("/api/projects/{id}/tasks", projectId)
            .then()
            .body("title", not(hasItem("Implement RAG Search")));
    }

    @Test
    @Order(6)
    void shouldCleanupProject() {
        if (projectId != null) {
            given()
                .when().delete("/api/projects/{id}", projectId)
                .then()
                .statusCode(204);
        }
    }
}
