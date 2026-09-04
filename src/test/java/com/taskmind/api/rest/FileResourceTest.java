package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileResourceTest {

    @Inject TestDataCleanup cleanup;

    private static Integer projectId;
    private static Integer taskId;
    private static Integer fileId;
    private static String fileName;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("fileuser-" + System.currentTimeMillis(), "file@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void prepareData() {
        // Create Project
        Response projResp = authenticated()
            .body(new ProjectRequest("file-project", null))
        .when().post("/api/projects")
        .then().statusCode(201).extract().response();
        projectId = projResp.jsonPath().getInt("id");

        // Create Task
        Response taskResp = authenticated()
            .body(new TaskRequest("Task for files", null, null))
        .when().post("/api/projects/" + projectId + "/tasks")
        .then().statusCode(201).extract().response();
        taskId = taskResp.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldUploadFile() throws IOException {
        Path tempFile = Files.createTempFile("test-upload", ".txt");
        Files.writeString(tempFile, "Hello Quarkus File Upload");
        File file = tempFile.toFile();

        // Note: We don't use authenticated() helper here because it sets Content-Type: application/json
        Response response = given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", file)
        .when()
            .post("/api/files/projects/{projectId}/tasks/{taskId}", projectId, taskId)
        .then()
            .statusCode(200)
            .body("fileOriginalName", containsString("test-upload"))
            .body("fileSize", equalTo((int) file.length()))
            .body("taskId", equalTo(taskId))
        .extract().response();

        fileId = response.jsonPath().getInt("id");
        fileName = response.jsonPath().getString("fileName");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    @Order(3)
    void shouldListFilesForTask() {
        authenticated()
        .when().get("/api/files/tasks/{taskId}", taskId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("id", hasItem(fileId));
    }

    @Test
    @Order(4)
    void shouldDownloadFile() {
        authenticated()
        .when().get("/api/files/download/{fileName}", fileName)
        .then()
            .statusCode(200)
            .header("Content-Disposition", containsString(fileName))
            .body(containsString("Hello Quarkus File Upload"));
    }

    @Test
    @Order(5)
    void shouldDeleteFile() {
        authenticated()
        .when().delete("/api/files/{fileId}", fileId)
        .then()
            .statusCode(204);

        authenticated()
        .when().get("/api/files/tasks/{taskId}", taskId)
        .then()
            .body("id", not(hasItem(fileId)));
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
