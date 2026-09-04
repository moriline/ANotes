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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Пункт 3 роадмапа: RBAC на трёх ручках, которые до этого не проверяли ничего —
 * {@code POST /api/projects/{id}/tasks}, {@code DELETE /api/projects/{id}/tasks/{taskId}}
 * и {@code DELETE /api/projects/{id}}.
 *
 * <p>Права берутся из справочника projectRoles:
 * {@code task:create} есть у Admin, Manager и Developer, {@code task:delete} —
 * у Admin и Manager, {@code project:delete} — только у Admin.
 */
@QuarkusTest
public class EndpointPermissionTest {

    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_MANAGER = 2;
    private static final int ROLE_DEVELOPER = 3;
    private static final int ROLE_GUEST = 4;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String strangerToken;
    private Integer projectId;
    private long ts;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        ts = System.nanoTime();
        ownerToken = register("rbac-owner");
        strangerToken = register("rbac-stranger");
        projectId = auth(ownerToken).body(new ProjectRequest("rbac-project-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    // --- POST /api/projects/{projectId}/tasks -------------------------------

    @Test
    public void ownerCanCreateTask() {
        createTask(ownerToken).statusCode(201);
    }

    @Test
    public void strangerCannotCreateTaskInSomebodyElsesProject() {
        createTask(strangerToken).statusCode(403);
    }

    @Test
    public void developerCanCreateTaskButGuestCannot() {
        String developerToken = memberWithRole("rbac-dev", ROLE_DEVELOPER);
        String guestToken = memberWithRole("rbac-guest", ROLE_GUEST);

        createTask(developerToken).statusCode(201);
        createTask(guestToken).statusCode(403);
    }

    /** Несуществующий проект прав тоже не даёт — и не подтверждает, что его нет. */
    @Test
    public void taskCannotBeCreatedInMissingProject() {
        auth(ownerToken).body(taskBody()).post("/api/projects/99999/tasks").then().statusCode(403);
    }

    // --- DELETE /api/projects/{projectId}/tasks/{taskId} --------------------

    @Test
    public void strangerCannotDeleteTaskAndTheTaskSurvives() {
        Integer taskId = createdTaskId();

        auth(strangerToken).delete(taskPath(taskId)).then().statusCode(403);
        auth(ownerToken).get(taskPath(taskId)).then().statusCode(200);
    }

    @Test
    public void managerCanDeleteTaskButDeveloperCannot() {
        String managerToken = memberWithRole("rbac-manager", ROLE_MANAGER);
        String developerToken = memberWithRole("rbac-dev2", ROLE_DEVELOPER);

        Integer forDeveloper = createdTaskId();
        auth(developerToken).delete(taskPath(forDeveloper)).then().statusCode(403);

        Integer forManager = createdTaskId();
        auth(managerToken).delete(taskPath(forManager)).then().statusCode(204);
        auth(ownerToken).get(taskPath(forManager)).then().statusCode(404);
    }

    /**
     * Задача из чужого проекта — 404, а не 204: раньше метод игнорировал projectId
     * из пути и удалял задачу по одному лишь taskId.
     */
    @Test
    public void taskOfAnotherProjectIsNotFound() {
        Integer taskId = createdTaskId();

        Integer otherProjectId = auth(ownerToken).body(new ProjectRequest("rbac-other-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).delete("/api/projects/" + otherProjectId + "/tasks/" + taskId)
            .then().statusCode(404);
        auth(ownerToken).get(taskPath(taskId)).then().statusCode(200);
    }

    // --- DELETE /api/projects/{projectId} -----------------------------------

    @Test
    public void strangerCannotDeleteProjectAndItSurvives() {
        auth(strangerToken).delete("/api/projects/" + projectId).then().statusCode(403);
        auth(ownerToken).get("/api/projects/" + projectId).then().statusCode(200);
    }

    /** project:delete есть только у Admin: Manager проект снести не может. */
    @Test
    public void managerCannotDeleteProjectButAdminCan() {
        String managerToken = memberWithRole("rbac-pm", ROLE_MANAGER);
        String adminToken = memberWithRole("rbac-padmin", ROLE_ADMIN);

        auth(managerToken).delete("/api/projects/" + projectId).then().statusCode(403);
        auth(adminToken).delete("/api/projects/" + projectId).then().statusCode(204);
    }

    @Test
    public void ownerCanDeleteHisOwnProject() {
        auth(ownerToken).delete("/api/projects/" + projectId).then().statusCode(204);
        auth(ownerToken).get("/api/projects/" + projectId).then().statusCode(404);
    }

    @Test
    public void missingProjectIsNotFound() {
        auth(ownerToken).delete("/api/projects/99999").then().statusCode(404);
    }

    @Test
    public void anonymousIsRejectedOnAllThree() {
        given().contentType(ContentType.JSON).body(taskBody())
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(401);
        given().delete("/api/projects/" + projectId + "/tasks/1").then().statusCode(401);
        given().delete("/api/projects/" + projectId).then().statusCode(401);
    }

    // --- helpers ------------------------------------------------------------

    private String register(String prefix) {
        return TestAuthHelper.registerAndLogin(prefix + "-" + ts, prefix + "-" + ts + "@test.com", "Pass123!");
    }

    /** Регистрирует пользователя и выдаёт ему роль в тестовом проекте. */
    private String memberWithRole(String prefix, int roleId) {
        String token = register(prefix);
        Integer id = auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body(new ProjectMemberRequest(id, roleId))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
        return token;
    }

    private io.restassured.response.ValidatableResponse createTask(String token) {
        return auth(token).body(taskBody()).post("/api/projects/" + projectId + "/tasks").then();
    }

    private Integer createdTaskId() {
        return createTask(ownerToken).statusCode(201).extract().jsonPath().getInt("id");
    }

    private TaskRequest taskBody() {
        return new TaskRequest("rbac task", "d", null);
    }

    private String taskPath(Integer taskId) {
        return "/api/projects/" + projectId + "/tasks/" + taskId;
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
