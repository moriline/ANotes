package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.CommentRequest;
import com.taskmind.api.dto.LoginRequest;
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
 * Чужой комментарий и чужой файл должны давать 403, а не 401.
 *
 * <p>Проверки авторства в CommentService и FileService кидали
 * {@code SecurityException}, а её маппер отдаёт 401 — то есть аутентифицированному
 * пользователю сообщалось «ты не залогинен», и клиент по такому ответу уходит
 * перелогиниваться вместо того, чтобы показать отказ. Путь не был покрыт тестами
 * вообще.
 */
@QuarkusTest
public class OwnershipStatusCodeTest {

    @Inject TestDataCleanup cleanup;

    private String authorToken;
    private String outsiderToken;
    private Integer projectId;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        authorToken = TestAuthHelper.registerAndLogin("own-author-" + ts, "own-author-" + ts + "@test.com", "Pass123!");
        outsiderToken = TestAuthHelper.registerAndLogin("own-other-" + ts, "own-other-" + ts + "@test.com", "Pass123!");

        projectId = auth(authorToken).body(new ProjectRequest("ownership-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
        taskId = auth(authorToken).body(new TaskRequest("ownership task", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void editingSomebodyElsesCommentIsForbiddenNotUnauthorized() {
        Integer commentId = createComment();

        auth(outsiderToken).body(new CommentRequest("hijacked", null))
            .put("/api/comments/" + commentId).then()
            .statusCode(403);

        // Комментарий должен остаться нетронутым.
        auth(authorToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200)
            .body("find { it.id == " + commentId + " }.content", equalTo("мой комментарий"))
            .body("find { it.id == " + commentId + " }.isEdited", equalTo(false));
    }

    @Test
    public void deletingSomebodyElsesCommentIsForbiddenAndItSurvives() {
        Integer commentId = createComment();

        auth(outsiderToken).delete("/api/comments/" + commentId).then().statusCode(403);

        auth(authorToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200)
            .body("id", hasItem(commentId));
    }

    @Test
    public void authorHimselfIsStillAllowed() {
        Integer commentId = createComment();

        auth(authorToken).body(new CommentRequest("поправил", null))
            .put("/api/comments/" + commentId).then()
            .statusCode(200)
            .body("content", equalTo("поправил"));
        auth(authorToken).delete("/api/comments/" + commentId).then().statusCode(204);
    }

    @Test
    public void deletingSomebodyElsesFileIsForbiddenAndItSurvives() throws IOException {
        Integer fileId = uploadFile();

        auth(outsiderToken).delete("/api/files/" + fileId).then().statusCode(403);

        auth(authorToken).get("/api/files/tasks/" + taskId).then()
            .statusCode(200)
            .body("id", hasItem(fileId));
    }

    @Test
    public void uploaderHimselfIsStillAllowed() throws IOException {
        Integer fileId = uploadFile();

        auth(authorToken).delete("/api/files/" + fileId).then().statusCode(204);
    }

    /** Неверный пароль по-прежнему 401: там SecurityException уместна. */
    @Test
    public void badCredentialsStayUnauthorized() {
        given().contentType(ContentType.JSON)
            .body(new LoginRequest("admin", "wrong-password"))
            .post("/api/auth/login").then().statusCode(401);
    }

    private Integer createComment() {
        return auth(authorToken).body(new CommentRequest("мой комментарий", null))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private Integer uploadFile() throws IOException {
        Path tempFile = Files.createTempFile("ownership-test", ".txt");
        Files.writeString(tempFile, "содержимое");
        File file = tempFile.toFile();
        try {
            return given().header("Authorization", "Bearer " + authorToken)
                .multiPart("file", file)
                .post("/api/files/projects/" + projectId + "/tasks/" + taskId).then()
                .statusCode(200)
                .extract().jsonPath().getInt("id");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
