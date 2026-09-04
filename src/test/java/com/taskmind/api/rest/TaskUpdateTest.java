package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        projectId = auth(ownerToken).body(new ProjectRequest("update-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        membershipRepository.save(ProjectMembership.create(projectId, memberId, ROLE_DEVELOPER));

        taskId = auth(ownerToken).body(new TaskRequest("Исходная задача", "описание", List.of("ai")))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        JsonPath statuses = auth(ownerToken).get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).extract().jsonPath();
        inProgressStatusId = statuses.getInt("[1].id");
        doneStatusId = statuses.getInt("[2].id");
    }

    @Test
    public void shouldAssignTaskToProjectMember() {
        patch(update().assignedUserId(memberId).build())
            .statusCode(200)
            .body("assignedUserId", equalTo(memberId))
            .body("title", equalTo("Исходная задача"));
    }

    @Test
    public void shouldMoveTaskThroughStatuses() {
        patch(update().statusId(inProgressStatusId).build())
            .statusCode(200)
            .body("statusId", equalTo(inProgressStatusId));

        patch(update().statusId(doneStatusId).build())
            .statusCode(200)
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldUpdateTextDatesAndEstimate() {
        patch(new TaskUpdateRequest("Новый заголовок", "новое описание", List.of("backend", "urgent"),
                null, null, 1767225600000L, 1764547200000L, 7.5, null))
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
        patch(update().assignedUserId(memberId).statusId(doneStatusId).build())
            .statusCode(200);

        // Второй PATCH меняет только заголовок — остальное обязано уцелеть.
        patch(update().title("Только заголовок").build())
            .statusCode(200)
            .body("title", equalTo("Только заголовок"))
            .body("description", equalTo("описание"))
            .body("tags", hasItem("ai"))
            .body("assignedUserId", equalTo(memberId))
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldArchiveTaskAndExposeItToTheArchivedFilter() {
        patch(update().isArchived(true).build())
            .statusCode(200)
            .body("isArchived", equalTo(true));

        auth(ownerToken).body(findArchived(true))
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        auth(ownerToken).body(findArchived(false))
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", not(hasItem(taskId)));
    }

    @Test
    public void assignedAndStatusFiltersOfFindNowReturnTheTask() {
        patch(update().assignedUserId(memberId).statusId(inProgressStatusId).build())
            .statusCode(200);

        auth(memberToken).body(findAssignedTo(memberId))
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        FindTasksRequest byStatus = new FindTasksRequest();
        byStatus.statusId = inProgressStatusId;
        auth(memberToken).body(byStatus)
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));

        auth(memberToken).body(findAssignedTo(ownerId))
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", not(hasItem(taskId)));
    }

    @Test
    public void shouldRejectAssigneeWhoIsNotInTheProject() {
        patch(update().assignedUserId(strangerId).build())
            .statusCode(400);
    }

    @Test
    public void shouldRejectAssigneeWhoDoesNotExist() {
        patch(update().assignedUserId(99999).build())
            .statusCode(400);
    }

    @Test
    public void shouldRejectStatusFromAnotherProject() {
        Integer otherProjectId = auth(strangerToken).body(new ProjectRequest("foreign-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        Integer foreignStatusId = auth(strangerToken)
            .get("/api/project-statuses/project/" + otherProjectId)
            .then().statusCode(200).extract().jsonPath().getInt("[0].id");

        patch(update().statusId(foreignStatusId).build())
            .statusCode(400);
    }

    @Test
    public void shouldReturn404WhenTaskDoesNotBelongToProjectInPath() {
        Integer otherProjectId = auth(ownerToken).body(new ProjectRequest("another-own-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body(update().title("нельзя").build())
            .patch("/api/projects/" + otherProjectId + "/tasks/" + taskId)
            .then().statusCode(404);
    }

    @Test
    public void shouldRejectCallerWithoutAccessToProject() {
        auth(strangerToken).body(update().title("чужое").build())
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(403);
    }

    @Test
    public void memberCanUpdateTaskOfTheProject() {
        auth(memberToken).body(update().statusId(doneStatusId).build())
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(200)
            .body("statusId", equalTo(doneStatusId));
    }

    @Test
    public void shouldRejectUnauthenticated() {
        given().contentType(ContentType.JSON).body(update().title("x").build())
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then().statusCode(401);
    }

    private io.restassured.response.ValidatableResponse patch(TaskUpdateRequest body) {
        return auth(ownerToken).body(body)
            .patch("/api/projects/" + projectId + "/tasks/" + taskId)
            .then();
    }

    private static FindTasksRequest findArchived(boolean archived) {
        FindTasksRequest query = new FindTasksRequest();
        query.isArchived = archived;
        return query;
    }

    private static FindTasksRequest findAssignedTo(Integer userId) {
        FindTasksRequest query = new FindTasksRequest();
        query.assignedUserId = userId;
        return query;
    }

    /** Собирает частичный {@link TaskUpdateRequest} — 9 позиционных null читаются плохо. */
    private static Update update() {
        return new Update();
    }

    private static final class Update {
        private String title;
        private Integer assignedUserId;
        private Integer statusId;
        private Boolean isArchived;

        Update title(String value) { this.title = value; return this; }
        Update assignedUserId(Integer value) { this.assignedUserId = value; return this; }
        Update statusId(Integer value) { this.statusId = value; return this; }
        Update isArchived(Boolean value) { this.isArchived = value; return this; }

        TaskUpdateRequest build() {
            return new TaskUpdateRequest(title, null, null, assignedUserId, statusId, null, null, null, isArchived);
        }
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
