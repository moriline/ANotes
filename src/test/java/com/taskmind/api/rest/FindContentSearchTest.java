package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
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

        projectId = auth(ownerToken).body("{\"name\": \"content-search-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        summaryTaskId = createTask("Задача с итогом");
        discussionTaskId = createTask("Задача с обсуждением");
        descriptionTaskId = createTask("Задача с описанием");

        // Вывод модели, записанный в summary.
        auth(ownerToken).body("{\"summary\": \"Причина найдена: ZZTOKENSUM в конфиге пула\"}")
            .put("/api/tasks/" + summaryTaskId + "/summary").then().statusCode(200);

        // Вывод модели, записанный блоком обсуждения.
        auth(ownerToken)
            .body("{\"author\": \"claude\", \"type\": \"DECISION\", \"content\": \"Решение: включить ZZTOKENDISC\"}")
            .post("/api/tasks/" + discussionTaskId + "/discussion").then().statusCode(201);

        auth(ownerToken).body("{\"description\": \"Здесь встречается ZZTOKENDESC\"}")
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
        auth(ownerToken).body("{\"isArchived\": true}")
            .patch("/api/projects/" + projectId + "/tasks/" + summaryTaskId).then().statusCode(200);

        auth(ownerToken).body("{\"contentSearch\": \"ZZTOKENSUM\", \"isArchived\": true}")
            .post("/api/find").then().statusCode(200)
            .body("tasks.id", contains(summaryTaskId));

        auth(ownerToken).body("{\"contentSearch\": \"ZZTOKENSUM\", \"isArchived\": false}")
            .post("/api/find").then().statusCode(200)
            .body("tasks", hasSize(0));

        auth(ownerToken).body("{\"contentSearch\": \"ZZTOKENSUM\", \"projectId\": " + projectId + "}")
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
        return auth(token).body("{\"contentSearch\": \"" + content + "\"}")
            .post("/api/find").then().statusCode(200);
    }

    private Integer createTask(String title) {
        return auth(ownerToken).body("{\"title\": \"" + title + "\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
