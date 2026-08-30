package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Новый проект должен приезжать с готовой доской «To Do → In Progress → Done»,
 * а созданная в нём задача — сразу вставать в первую колонку.
 */
@QuarkusTest
public class ProjectDefaultStatusTest {

    @Inject TestDataCleanup cleanup;

    private String token;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        token = TestAuthHelper.registerAndLogin(
            "status_default_user", "status_default@test.com", "Pass123!");
    }

    @Test
    public void newProjectGetsDefaultBoardInOrder() {
        Integer projectId = createProject("board-project");

        JsonPath statuses = statusesOf(projectId);

        assertEquals(3, statuses.getList("$").size());
        assertEquals(java.util.List.of("To Do", "In Progress", "Done"), statuses.getList("statusName"));
        assertEquals(java.util.List.of(0, 1, 2), statuses.getList("statusOrder"));
        assertEquals(java.util.List.of("#95A5A6", "#3498DB", "#2ECC71"), statuses.getList("statusColor"));
    }

    @Test
    public void defaultBoardHasExactlyOneEntryColumnAndOneClosingColumn() {
        Integer projectId = createProject("board-flags-project");

        JsonPath statuses = statusesOf(projectId);

        assertEquals(java.util.List.of(true, false, false), statuses.getList("isDefault"),
            "входная колонка должна быть ровно одна — To Do");
        assertEquals(java.util.List.of(false, false, true), statuses.getList("isClosed"),
            "закрывающая колонка должна быть ровно одна — Done");
    }

    @Test
    public void newTaskLandsInTheDefaultColumn() {
        Integer projectId = createProject("board-task-project");
        Integer toDoStatusId = statusesOf(projectId).getInt("[0].id");

        authenticated()
            .body("{\"title\": \"Задача на доске\"}")
        .when().post("/api/projects/" + projectId + "/tasks")
        .then()
            .statusCode(201)
            .body("statusId", equalTo(toDoStatusId));
    }

    @Test
    public void everyProjectGetsItsOwnBoard() {
        Integer first = createProject("board-one");
        Integer second = createProject("board-two");

        JsonPath firstStatuses = statusesOf(first);
        JsonPath secondStatuses = statusesOf(second);

        assertEquals(3, firstStatuses.getList("$").size());
        assertEquals(3, secondStatuses.getList("$").size());
        assertNotEquals(firstStatuses.getList("id"), secondStatuses.getList("id"),
            "статусы разных проектов — разные строки");
        assertEquals(java.util.List.of(second, second, second), secondStatuses.getList("projectId"));
    }

    @Test
    public void existingProjectsKeepTheirOwnStatuses() {
        // Проект 1 из сида живёт со своей доской Backlog/In Progress/Review/Done,
        // дефолты не должны в неё вмешиваться.
        authenticated()
        .when().get("/api/project-statuses/project/1")
        .then()
            .statusCode(200)
            .body("statusName", contains("Backlog", "In Progress", "Review", "Done"));
    }

    private Integer createProject(String name) {
        return authenticated()
            .body("{\"name\": \"" + name + "\"}")
        .when().post("/api/projects")
        .then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private JsonPath statusesOf(Integer projectId) {
        return authenticated()
            .when().get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200)
            .extract().jsonPath();
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
