package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectResourceTest {

    private static final String TEST_PROJECT_NAME = "stage1-test-project";
    private static String createdProjectId;
    private static Path projectDir;

    @Test
    @Order(1)
    void shouldCreateProjectAndDirectoryStructure() {
        Response response = given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + TEST_PROJECT_NAME + "\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(201)
            .body("name", equalTo(TEST_PROJECT_NAME))
            .body("status", equalTo("ACTIVE"))
            .body("id", notNullValue())
            .body("rootPath", notNullValue())
        .extract().response();

        createdProjectId = response.jsonPath().getString("id");

        String sanitized = TEST_PROJECT_NAME.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        projectDir = Path.of(System.getProperty("user.dir"), "projects-test", sanitized);

        assertTrue(Files.exists(projectDir), "Project directory should exist");
        assertTrue(Files.isDirectory(projectDir.resolve("raw")), "raw/ directory should exist");
        assertTrue(Files.isDirectory(projectDir.resolve("wiki")), "wiki/ directory should exist");
        assertTrue(Files.isDirectory(projectDir.resolve("tasks")), "tasks/ directory should exist");
        assertTrue(Files.isDirectory(projectDir.resolve("archives")), "archives/ directory should exist");
        assertTrue(Files.exists(projectDir.resolve("AGENTS.md")), "AGENTS.md should exist");
        assertTrue(Files.exists(projectDir.resolve("project.json")), "project.json should exist");

        String jsonContent = assertDoesNotThrow(() -> Files.readString(projectDir.resolve("project.json")));
        assertTrue(jsonContent.contains("\"name\""), "project.json should contain name field");
        assertTrue(jsonContent.contains("\"id\""), "project.json should contain id field");
        assertTrue(jsonContent.contains("\"status\""), "project.json should contain status field");
    }

    @Test
    @Order(2)
    void shouldListActiveProjects() {
        given()
        .when()
            .get("/api/projects")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("name", hasItem(TEST_PROJECT_NAME));
    }

    @Test
    @Order(3)
    void shouldRejectDuplicateName() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + TEST_PROJECT_NAME + "\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(500);
    }

    @Test
    @Order(4)
    void shouldDeleteProject() {
        given()
        .when()
            .delete("/api/projects/" + createdProjectId)
        .then()
            .statusCode(204);

        given()
        .when().get("/api/projects")
        .then()
            .body("name", not(hasItem(TEST_PROJECT_NAME)));

        assertFalse(Files.exists(projectDir), "Project directory should be deleted");
    }
}
