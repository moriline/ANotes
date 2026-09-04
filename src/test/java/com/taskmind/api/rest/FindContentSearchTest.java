package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.DiscussionBlockRequest;
import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskSummaryRequest;
import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.domain.model.BlockType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * {@code contentSearch} в /api/find — то, чем модель находит собственные прошлые
 * выводы: поиск идёт по заголовку, описанию, summary и обсуждению разом.
 */
@QuarkusTest
public class FindContentSearchTest {

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String strangerToken;
    private Integer projectId;
    private Integer summaryTaskId;
    private Integer discussionTaskId;
    private Integer descriptionTaskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("find-owner-" + ts, "find-owner-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("find-stranger-" + ts, "find-stranger-" + ts + "@test.com", "Pass123!");

        projectId = auth(ownerToken).body(new ProjectRequest("content-search-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        summaryTaskId = createTask("Задача с итогом");
        discussionTaskId = createTask("Задача с обсуждением");
        descriptionTaskId = createTask("Задача с описанием");

        // Вывод модели, записанный в summary.
        auth(ownerToken).body(new TaskSummaryRequest("Причина найдена: ZZTOKENSUM в конфиге пула"))
            .put("/api/tasks/" + summaryTaskId + "/summary").then().statusCode(200);

        // Вывод модели, записанный блоком обсуждения.
        auth(ownerToken)
            .body(new DiscussionBlockRequest(null, null, "claude", BlockType.DECISION, "Решение: включить ZZTOKENDISC"))
            .post("/api/tasks/" + discussionTaskId + "/discussion").then().statusCode(201);

        auth(ownerToken).body(new TaskUpdateRequest(null, "Здесь встречается ZZTOKENDESC", null, null, null, null, null, null, null))
            .patch("/api/projects/" + projectId + "/tasks/" + descriptionTaskId).then().statusCode(200);
    }

    @Test
    public void findsTaskByTextWrittenIntoSummary() {
        search(ownerToken, "ZZTOKENSUM")
            .body("tasks.id", contains(summaryTaskId))
            .body("tasks[0].summary", containsString("ZZTOKENSUM"));
    }

    @Test
    public void findsTaskByTextWrittenIntoDiscussion() {
        search(ownerToken, "ZZTOKENDISC")
            .body("tasks.id", contains(discussionTaskId));
    }

    @Test
    public void findsTaskByDescription() {
        search(ownerToken, "ZZTOKENDESC")
            .body("tasks.id", contains(descriptionTaskId));
    }

    @Test
    public void findsTaskByTitleToo() {
        search(ownerToken, "Задача с итогом")
            .body("tasks.id", contains(summaryTaskId));
    }

    @Test
    public void findsDiscussionByAuthorAndByBlockType() {
        // Поиск идёт по сырому JSON обсуждения, поэтому ловит и автора, и тип блока.
        search(ownerToken, "claude").body("tasks.id", contains(discussionTaskId));
        search(ownerToken, "DECISION").body("tasks.id", contains(discussionTaskId));
    }

    @Test
    public void searchIsCaseInsensitive() {
        search(ownerToken, "zztokensum").body("tasks.id", contains(summaryTaskId));
        search(ownerToken, "ZzToKeNdIsC").body("tasks.id", contains(discussionTaskId));
    }

    @Test
    public void contentSearchIsCombinedWithOtherFiltersByAnd() {
        auth(ownerToken).body(new TaskUpdateRequest(null, null, null, null, null, null, null, null, true))
            .patch("/api/projects/" + projectId + "/tasks/" + summaryTaskId).then().statusCode(200);

        FindTasksRequest archivedMatch = contentSearch("ZZTOKENSUM");
        archivedMatch.isArchived = true;
        auth(ownerToken).body(archivedMatch)
            .post("/api/find").then().statusCode(200)
            .body("tasks.id", contains(summaryTaskId));

        FindTasksRequest activeMatch = contentSearch("ZZTOKENSUM");
        activeMatch.isArchived = false;
        auth(ownerToken).body(activeMatch)
            .post("/api/find").then().statusCode(200)
            .body("tasks", hasSize(0));

        FindTasksRequest projectMatch = contentSearch("ZZTOKENSUM");
        projectMatch.projectId = projectId;
        auth(ownerToken).body(projectMatch)
            .post("/api/find").then().statusCode(200)
            .body("tasks.id", contains(summaryTaskId));
    }

    @Test
    public void searchStaysInsideTheCallersProjects() {
        search(strangerToken, "ZZTOKENSUM").body("tasks", hasSize(0));
        search(strangerToken, "ZZTOKENDISC").body("tasks", hasSize(0));
    }

    @Test
    public void unmatchedTextReturnsNothing() {
        search(ownerToken, "ZZNOTHINGHERE").body("tasks", hasSize(0));
    }

    private io.restassured.response.ValidatableResponse search(String token, String content) {
        return auth(token).body(contentSearch(content))
            .post("/api/find").then().statusCode(200);
    }

    /** {@code contentSearch}-фильтр, к которому тесты дописывают остальные поля. */
    private static FindTasksRequest contentSearch(String content) {
        FindTasksRequest query = new FindTasksRequest();
        query.contentSearch = content;
        return query;
    }

    private Integer createTask(String title) {
        return auth(ownerToken).body(new TaskRequest(title, null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
