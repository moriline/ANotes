package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * PATCH /api/projects/{projectId}/tasks/{taskId}: назначение исполнителя,
 * перевод по статусам, сроки и архивация. До появления этого эндпоинта
 * assignedUserId и statusId существовали только в базе, а фильтры /api/find по
 * ним были мёртвым кодом.
 */
@QuarkusTest
public class TaskUpdateTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;
    @Inject MembershipRepository membershipRepository;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer ownerId;
    private Integer memberId;
    private Integer strangerId;
    private Integer projectId;
    private Integer taskId;
    private Integer inProgressStatusId;
    private Integer doneStatusId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("upd-owner-" + ts, "upd-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("upd-member-" + ts, "upd-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("upd-stranger-" + ts, "upd-stranger-" + ts + "@test.com", "Pass123!");

        ownerId = meId(ownerToken);
        memberId = meId(memberToken);
        strangerId = meId(strangerToken);

        projectId = auth(ownerToken).body("{\"name\": \"update-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        membershipRepository.save(ProjectMembership.create(projectId, memberId, ROLE_DEVELOPER));

        taskId = auth(ownerToken).body("{\"title\": \"Исходная задача\", \"description\": \"описание\", \"tags\": [\"ai\"]}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        JsonPath statuses = auth(ownerToken).get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).extract().jsonPath();
        inProgressStatusId = statuses.getInt("[1].id");
        doneStatusId = statuses.getInt("[2].id");
    }

    @Test
    public void shouldAssignTaskToProjectMember() {
        patch("{\"assignedUserId\": " + memberId + "}")
            .statusCode(200)
            .body("assignedUserId", equalTo(memberId))
            .body("title", equalTo("Исходная задача"));
    }

    @Test
    public void shouldMoveTaskThroughStatuses() {
        patch("{\"statusId\": " + inProgressStatusId + "}")
            .statusCode(200)
            .body("statusId", equalTo(inProgressStatusId));

        patch("{\"statusId\": " + doneStatusId + "}")
            .statusCode(200)
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldUpdateTextDatesAndEstimate() {
        patch("{\"title\": \"Новый заголовок\", \"description\": \"новое описание\","
            + " \"tags\": [\"backend\", \"urgent\"], \"dueDate\": 1767225600000,"
            + " \"startDate\": 1764547200000, \"estimatedHours\": 7.5}")
            .statusCode(200)
            .body("title", equalTo("Новый заголовок"))
            .body("description", equalTo("новое описание"))
            .body("tags", hasItems("backend", "urgent"))
            .body("dueDate", equalTo(1767225600000L))
            .body("startDate", equalTo(1764547200000L))
            .body("estimatedHours", equalTo(7.5f));
    }

    @Test
    public void shouldLeaveOmittedFieldsUntouched() {
        patch("{\"assignedUserId\": " + memberId + ", \"statusId\": " + doneStatusId + "}")
            .statusCode(200);

        // Второй PATCH меняет только заголовок — остальное обязано уцелеть.
        patch("{\"title\": \"Только заголовок\"}")
            .statusCode(200)
            .body("title", equalTo("Только заголовок"))
            .body("description", equalTo("описание"))
            .body("tags", hasItem("ai"))
            .body("assignedUserId", equalTo(memberId))
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldArchiveTaskAndExposeItToTheArchivedFilter() {
        patch("{\"isArchived\": true}")
            .statusCode(200)
            .body("isArchived", equalTo(true));

        auth(ownerToken).body("{\"isArchived\": true}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        auth(ownerToken).body("{\"isArchived\": false}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", not(hasItem(taskId)));
    }

    @Test
    public void assignedAndStatusFiltersOfFindNowReturnTheTask() {
        patch("{\"assignedUserId\": " + memberId + ", \"statusId\": " + inProgressStatusId + "}")
            .statusCode(200);

        auth(memberToken).body("{\"assignedUserId\": " + memberId + "}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        auth(memberToken).body("{\"statusId\": " + inProgressStatusId + "}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        auth(memberToken).body("{\"assignedUserId\": " + ownerId + "}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", not(hasItem(taskId)));
    }

    @Test
    public void shouldRejectAssigneeWhoIsNotInTheProject() {
        patch("{\"assignedUserId\": " + strangerId + "}")
            .statusCode(400);
    }

    @Test
    public void shouldRejectAssigneeWhoDoesNotExist() {
        patch("{\"assignedUserId\": 99999}")
            .statusCode(400);
    }

    @Test
    public void shouldRejectStatusFromAnotherProject() {
        Integer otherProjectId = auth(strangerToken).body("{\"name\": \"foreign-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        Integer foreignStatusId = auth(strangerToken)
            .get("/api/project-statuses/project/" + otherProjectId)
            .then().statusCode(200).extract().jsonPath().getInt("[0].id");

        patch("{\"statusId\": " + foreignStatusId + "}")
            .statusCode(400);
    }

    @Test
    public void shouldReturn404WhenTaskDoesNotBelongToProjectInPath() {
        Integer otherProjectId = auth(ownerToken).body("{\"name\": \"another-own-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body("{\"title\": \"нельзя\"}")
            .patch("/api/projects/" + otherProjectId + "/tasks/" + taskId)
            .then().statusCode(404);
    }

    @Test
    public void shouldRejectCallerWithoutAccessToProject() {
        auth(strangerToken).body("{\"title\": \"чужое\"}")
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(403);
    }

    @Test
    public void memberCanUpdateTaskOfTheProject() {
        auth(memberToken).body("{\"statusId\": " + doneStatusId + "}")
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(200)
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldRejectUnauthenticated() {
        given().contentType(ContentType.JSON).body("{\"title\": \"x\"}")
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(401);
    }

    private io.restassured.response.ValidatableResponse patch(String body) {
        return auth(ownerToken).body(body)
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then();
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
