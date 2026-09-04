package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.CommentRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Доступ к {@code /api/tasks/{taskId}/comments} и {@code /api/comments/{id}}.
 *
 * <p>Раньше проверки не было: любой залогиненный пользователь читал и писал
 * комментарии к задаче любого проекта. Теперь и то и другое — только для
 * участника проекта. Создатель проекта участником становится сам, с ролью Admin.
 */
@QuarkusTest
public class TaskCommentAccessTest {

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
        ownerToken = TestAuthHelper.registerAndLogin("ca-owner-" + ts, "ca-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("ca-member-" + ts, "ca-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("ca-stranger-" + ts, "ca-stranger-" + ts + "@test.com", "Pass123!");
        memberId = meId(memberToken);

        projectId = auth(ownerToken).body(new ProjectRequest("comment-access-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
        taskId = auth(ownerToken).body(new TaskRequest("task", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void projectCreatorCanReadAndWriteComments() {
        // Создатель нигде не добавлялся руками — доступ есть только потому, что
        // createProject кладёт его в участники с ролью Admin.
        Integer commentId = auth(ownerToken).body(new CommentRequest("первый", null))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201)
            .extract().path("id");

        auth(ownerToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200).body("id", hasItem(commentId));
    }

    @Test
    public void strangerCannotReadOrWriteComments() {
        auth(strangerToken).get("/api/tasks/" + taskId + "/comments").then().statusCode(403);

        auth(strangerToken).body(new CommentRequest("чужой проект", null))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(403);
    }

    @Test
    public void addedMemberCanComment() {
        addMember(memberId, ROLE_DEVELOPER);

        auth(memberToken).body(new CommentRequest("я в проекте", null))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201);
        auth(memberToken).get("/api/tasks/" + taskId + "/comments").then().statusCode(200);
    }

    @Test
    public void removedMemberCannotTouchOwnComment() {
        addMember(memberId, ROLE_DEVELOPER);

        Integer commentId = auth(memberToken).body(new CommentRequest("пока я здесь", null))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201)
            .extract().path("id");

        auth(ownerToken).delete("/api/projects/" + projectId + "/members/" + memberId)
            .then().statusCode(204);

        // Свой комментарий, но проект уже недоступен -> 403, а не 200.
        auth(memberToken).body(new CommentRequest("правка после выхода", null))
            .put("/api/comments/" + commentId).then().statusCode(403);
        auth(memberToken).delete("/api/comments/" + commentId).then().statusCode(403);

        // Владелец проекта его по-прежнему видит.
        auth(ownerToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200).body("id", hasItem(commentId));
    }

    @Test
    public void commentingOnAMissingTaskIsNotFound() {
        auth(ownerToken).body(new CommentRequest("нет задачи", null))
            .post("/api/tasks/999999/comments").then().statusCode(404);
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
