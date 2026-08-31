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
 * Обращение по несуществующему id должно давать 404.
 *
 * <p>{@code PUT /api/comments/{id}} кидал IllegalArgumentException, маппера на
 * неё нет — клиент получал 500, то есть «сервер сломался» вместо «такого нет».
 * Соседние DELETE комментария и файла в том же случае молча отвечали 204, будто
 * удаление удалось.
 */
@QuarkusTest
public class MissingResourceStatusTest {

    private static final int MISSING_ID = 999999;

    @Inject TestDataCleanup cleanup;

    private String token;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        token = TestAuthHelper.registerAndLogin("missing-" + ts, "missing-" + ts + "@test.com", "Pass123!");

        Integer projectId = auth().body("{\"name\": \"missing-" + ts + "\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
        taskId = auth().body("{\"title\": \"task\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void editingMissingCommentIsNotFoundNotServerError() {
        auth().body("{\"content\": \"текст\"}")
            .put("/api/comments/" + MISSING_ID).then()
            .statusCode(404);
    }

    @Test
    public void deletingMissingCommentIsNotFoundNotNoContent() {
        auth().delete("/api/comments/" + MISSING_ID).then().statusCode(404);
    }

    @Test
    public void deletingMissingFileIsNotFoundNotNoContent() {
        auth().delete("/api/files/" + MISSING_ID).then().statusCode(404);
    }

    /** Контроль: существующий комментарий по-прежнему правится и удаляется. */
    @Test
    public void existingCommentIsStillEditableAndDeletable() {
        Integer commentId = auth().body("{\"content\": \"исходный\"}")
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth().body("{\"content\": \"поправленный\"}")
            .put("/api/comments/" + commentId).then()
            .statusCode(200)
            .body("content", equalTo("поправленный"));

        auth().delete("/api/comments/" + commentId).then().statusCode(204);

        // А повторное удаление того же id — уже 404, а не бесшумный 204.
        auth().delete("/api/comments/" + commentId).then().statusCode(404);
    }

    private RequestSpecification auth() {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
