package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Доступ к {@code /api/files/**}. Раньше проверок не было: файл скачивался по
 * одному имени, список — по одному taskId, загрузка и удаление не смотрели на
 * проект. Теперь всё это — только для участника проекта, к задаче которого файл
 * относится. Создатель проекта участник по умолчанию.
 */
@QuarkusTest
public class FileAccessTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer memberId;
    private Integer projectId;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("fa-owner-" + ts, "fa-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("fa-member-" + ts, "fa-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("fa-stranger-" + ts, "fa-stranger-" + ts + "@test.com", "Pass123!");
        memberId = meId(memberToken);

        projectId = auth(ownerToken).body(new ProjectRequest("file-access-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
        taskId = auth(ownerToken).body(new TaskRequest("task", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void strangerCannotUploadListOrDownload() throws IOException {
        String fileName = upload(ownerToken).getString("fileName");

        uploadRequest(strangerToken).post("/api/files/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(403);
        auth(strangerToken).get("/api/files/tasks/" + taskId).then().statusCode(403);
        auth(strangerToken).get("/api/files/download/" + fileName).then().statusCode(403);
    }

    @Test
    public void addedMemberCanUploadListAndDownload() throws IOException {
        addMember(memberId, ROLE_DEVELOPER);

        String fileName = upload(memberToken).getString("fileName");
        auth(memberToken).get("/api/files/tasks/" + taskId).then().statusCode(200);
        auth(memberToken).get("/api/files/download/" + fileName).then().statusCode(200);
    }

    @Test
    public void removedMemberLosesAccessToOwnFile() throws IOException {
        addMember(memberId, ROLE_DEVELOPER);
        var uploaded = upload(memberToken);
        int fileId = uploaded.getInt("id");
        String fileName = uploaded.getString("fileName");

        auth(ownerToken).delete("/api/projects/" + projectId + "/members/" + memberId)
            .then().statusCode(204);

        auth(memberToken).get("/api/files/download/" + fileName).then().statusCode(403);
        auth(memberToken).delete("/api/files/" + fileId).then().statusCode(403);
        // Владелец файл по-прежнему видит.
        auth(ownerToken).get("/api/files/tasks/" + taskId).then()
            .statusCode(200).body("id", hasItem(fileId));
    }

    @Test
    public void uploadingUnderTheWrongProjectIs404() throws IOException {
        Integer otherProject = auth(ownerToken).body(new ProjectRequest("fa-other-" + System.nanoTime(), null))
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");

        uploadRequest(ownerToken).post("/api/files/projects/" + otherProject + "/tasks/" + taskId)
            .then().statusCode(404);
    }

    private io.restassured.path.json.JsonPath upload(String token) throws IOException {
        return uploadRequest(token).post("/api/files/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(200).extract().jsonPath();
    }

    private RequestSpecification uploadRequest(String token) throws IOException {
        Path tempFile = Files.createTempFile("file-access", ".txt");
        Files.writeString(tempFile, "содержимое вложения");
        File file = tempFile.toFile();
        file.deleteOnExit();
        return given().header("Authorization", "Bearer " + token).multiPart("file", file);
    }

    private void addMember(Integer userId, int roleId) {
        auth(ownerToken).body(new ProjectMemberRequest(userId, roleId))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
