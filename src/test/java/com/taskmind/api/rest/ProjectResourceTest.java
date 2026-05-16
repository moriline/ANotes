package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectResourceTest {

    @Inject TestDataCleanup cleanup;

    private static final String TEST_PROJECT_NAME = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private static String createdProjectId;
    private static Path projectDir;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("projuser-" + System.currentTimeMillis(), "proj@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void shouldCreateProjectAndDirectoryStructure() {
        Response response = authenticated()
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
        projectDir = Path.of("D:/projects/java/ANotes/projects-test", sanitized);

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
        authenticated()
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
        authenticated()
            .body("{\"name\": \"" + TEST_PROJECT_NAME + "\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(500);
    }

    @Test
    @Order(4)
    void shouldDeleteProject() {
        authenticated()
        .when()
            .delete("/api/projects/" + createdProjectId)
        .then()
            .statusCode(204);

        authenticated()
        .when().get("/api/projects")
        .then()
            .body("name", not(hasItem(TEST_PROJECT_NAME)));

        assertFalse(Files.exists(projectDir), "Project directory should be deleted");
    }

    @Test
    @Order(5)
    void shouldRejectUnauthenticatedAccess() {
        given()
            .contentType(io.restassured.http.ContentType.JSON)
            .body("{\"name\": \"unauth-project\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(401);
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
