package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.CommentRequest;
import com.taskmind.api.dto.DiscussionBlockRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskSummaryRequest;
import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.api.dto.TimeEntryRequest;
import com.taskmind.domain.model.Visibility;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Роль «Заказчик» (roleId 6): по правам как Guest — только чтение задач — но это
 * внешняя роль. Заказчик комментирует (его комментарии всегда PUBLIC), видит
 * только PUBLIC-комментарии и события ленты, и не может менять задачи, писать
 * итог/обсуждение и списывать время.
 */
@QuarkusTest
public class ClientRoleTest {

    private static final int ROLE_DEVELOPER = 3;
    private static final int ROLE_CLIENT = 6;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String clientToken;
    private String devToken;
    private Integer projectId;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("cl-owner-" + ts, "cl-owner-" + ts + "@test.com", "Pass123!");
        clientToken = TestAuthHelper.registerAndLogin("cl-client-" + ts, "cl-client-" + ts + "@test.com", "Pass123!");
        devToken = TestAuthHelper.registerAndLogin("cl-dev-" + ts, "cl-dev-" + ts + "@test.com", "Pass123!");

        projectId = auth(ownerToken).body(new ProjectRequest("client-role-" + ts, null))
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");
        taskId = auth(ownerToken).body(new TaskRequest("Задача проекта", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body(new ProjectMemberRequest(meId(devToken), ROLE_DEVELOPER))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
    }

    @Test
    public void clientRoleCanBeAssignedAndCarriesItsName() {
        auth(ownerToken).body(new ProjectMemberRequest(meId(clientToken), ROLE_CLIENT))
            .post("/api/projects/" + projectId + "/members").then()
            .statusCode(201)
            .body("roleId", equalTo(ROLE_CLIENT))
            .body("roleName", equalTo("Client"));
    }

    @Test
    public void clientReadsTasksBoardAndComments() {
        addClient();

        auth(clientToken).get("/api/projects/" + projectId).then().statusCode(200);
        auth(clientToken).get("/api/projects/" + projectId + "/tasks").then().statusCode(200)
            .body("id", hasItem(taskId));
        auth(clientToken).get("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(200);
        auth(clientToken).get("/api/project-statuses/project/" + projectId).then().statusCode(200);
        auth(clientToken).get("/api/tasks/" + taskId + "/comments").then().statusCode(200);
        auth(clientToken).get("/api/projects/" + projectId + "/activity").then().statusCode(200);
        auth(clientToken).get("/api/tasks/" + taskId + "/time").then().statusCode(200);
    }

    @Test
    public void clientCannotChangeAnything() {
        addClient();

        auth(clientToken).body(new TaskRequest("нельзя", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(403);
        auth(clientToken).body(new TaskUpdateRequest("переименовал", null, null, null, null, null, null, null, null))
            .patch("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(403);
        auth(clientToken).delete("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(403);
        auth(clientToken).body(new TaskSummaryRequest("мой вывод"))
            .put("/api/tasks/" + taskId + "/summary").then().statusCode(403);
        auth(clientToken).body(new DiscussionBlockRequest(null, null, null, null, "моя мысль"))
            .post("/api/tasks/" + taskId + "/discussion").then().statusCode(403);
        auth(clientToken).body(new TimeEntryRequest(600L, null, null))
            .post("/api/tasks/" + taskId + "/time").then().statusCode(403);
        auth(clientToken).body(new ProjectMemberRequest(meId(ownerToken), ROLE_DEVELOPER))
            .post("/api/projects/" + projectId + "/members").then().statusCode(403);
    }

    @Test
    public void clientSeesOnlyPublicCommentsTeamSeesAll() {
        addClient();
        auth(ownerToken).body(new CommentRequest("виден заказчику", Visibility.PUBLIC))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201);
        auth(ownerToken).body(new CommentRequest("только для команды", Visibility.INTERNAL))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201);

        auth(clientToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200)
            .body("content", contains("виден заказчику"))
            .body("visibility", everyItem(is("PUBLIC")));

        auth(devToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200)
            .body("content", hasItems("виден заказчику", "только для команды"));
    }

    @Test
    public void clientCommentIsAlwaysPublicEvenWhenInternalRequested() {
        addClient();

        Integer commentId = auth(clientToken)
            .body(new CommentRequest("прошу пояснить сроки", Visibility.INTERNAL))
            .post("/api/tasks/" + taskId + "/comments").then()
            .statusCode(201)
            .body("visibility", is("PUBLIC"))
            .extract().jsonPath().getInt("id");

        // Правка собственного комментария тоже не даёт заказчику сделать его внутренним.
        auth(clientToken).body(new CommentRequest("прошу пояснить сроки и бюджет", Visibility.INTERNAL))
            .put("/api/comments/" + commentId).then()
            .statusCode(200)
            .body("visibility", is("PUBLIC"));

        // Команда этот комментарий видит — он публичный.
        auth(devToken).get("/api/tasks/" + taskId + "/comments").then()
            .statusCode(200)
            .body("id", hasItem(commentId));
    }

    @Test
    public void clientActivityFeedHidesInternalEvents() {
        addClient();
        auth(ownerToken).body(new CommentRequest("публичный апдейт", Visibility.PUBLIC))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201);
        auth(ownerToken).body(new CommentRequest("внутренняя заметка", Visibility.INTERNAL))
            .post("/api/tasks/" + taskId + "/comments").then().statusCode(201);

        // Заказчик: только публичные события, внутреннего COMMENT_ADDED в ленте нет.
        auth(clientToken).get("/api/projects/" + projectId + "/activity").then()
            .statusCode(200)
            .body("visibility", everyItem(is("PUBLIC")))
            .body("actionType", hasItems("PROJECT_CREATED", "TASK_CREATED", "COMMENT_ADDED"))
            .body("details.excerpt", not(hasItem("внутренняя заметка")));

        auth(clientToken).get("/api/tasks/" + taskId + "/activity").then()
            .statusCode(200)
            .body("visibility", everyItem(is("PUBLIC")));

        // Команда видит и внутреннее событие.
        auth(devToken).get("/api/projects/" + projectId + "/activity").then()
            .statusCode(200)
            .body("visibility", hasItem("INTERNAL"))
            .body("details.excerpt", hasItem("внутренняя заметка"));
    }

    private void addClient() {
        auth(ownerToken).body(new ProjectMemberRequest(meId(clientToken), ROLE_CLIENT))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
