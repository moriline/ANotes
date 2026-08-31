package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Лента активности: {@code GET /api/projects/{id}/activity} и
 * {@code GET /api/tasks/{id}/activity}. Таблица activityLog существовала с самого
 * начала, но её никто не заполнял — в ней были только четыре строки из сида.
 */
@QuarkusTest
public class ActivityLogTest {

    private static final int ROLE_DEVELOPER = 3;
    private static final int ROLE_MANAGER = 2;
    private static final int SEEDED_PROJECT_ID = 1;

    @Inject TestDataCleanup cleanup;
    @Inject MembershipRepository membershipRepository;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private String ownerUsername;
    private Integer ownerId;
    private Integer memberId;
    private Integer projectId;
    private Integer taskId;
    private Integer inProgressStatusId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerUsername = "act-owner-" + ts;
        ownerToken = TestAuthHelper.registerAndLogin(ownerUsername, ownerUsername + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("act-member-" + ts, "act-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("act-stranger-" + ts, "act-stranger-" + ts + "@test.com", "Pass123!");

        ownerId = meId(ownerToken);
        memberId = meId(memberToken);

        projectId = auth(ownerToken).body("{\"name\": \"activity-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        taskId = auth(ownerToken).body("{\"title\": \"Задача для ленты\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        inProgressStatusId = auth(ownerToken).get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).extract().jsonPath().getInt("[1].id");
    }

    @Test
    public void projectCreationAndTaskCreationAreLogged() {
        List<Map<String, Object>> feed = projectFeed(ownerToken);

        assertTrue(actionTypes(feed).containsAll(List.of("PROJECT_CREATED", "MEMBER_ADDED", "TASK_CREATED")),
            "в ленте должны быть создание проекта, добавление владельца и создание задачи: " + actionTypes(feed));

        Map<String, Object> created = firstOfType(feed, "TASK_CREATED");
        assertEquals(taskId, created.get("taskId"));
        assertEquals(ownerId, created.get("userId"));
        assertEquals(ownerUsername, created.get("username"));
        assertEquals("Задача для ленты", details(created).get("title"));
    }

    @Test
    public void assigningATaskIsLoggedWithFromAndTo() {
        addMember(memberId, ROLE_DEVELOPER);

        auth(ownerToken).body("{\"assignedUserId\": " + memberId + "}")
            .patch(taskPath()).then().statusCode(200);

        Map<String, Object> event = firstOfType(projectFeed(ownerToken), "ASSIGNEE_UPDATED");
        assertNull(details(event).get("from"), "исполнителя не было");
        assertEquals(memberId, details(event).get("to"));
    }

    @Test
    public void movingATaskBetweenStatusesIsLogged() {
        auth(ownerToken).body("{\"statusId\": " + inProgressStatusId + "}")
            .patch(taskPath()).then().statusCode(200);

        Map<String, Object> event = firstOfType(projectFeed(ownerToken), "STATUS_CHANGED");
        assertEquals(inProgressStatusId, details(event).get("to"));
        assertNotNull(details(event).get("from"), "задача создаётся уже в первой колонке доски");
    }

    @Test
    public void editingPlainFieldsLogsOneUpdateWithTheFieldNames() {
        auth(ownerToken).body("{\"title\": \"Новый заголовок\", \"estimatedHours\": 3.0}")
            .patch(taskPath()).then().statusCode(200);

        List<Map<String, Object>> feed = projectFeed(ownerToken);
        assertFalse(actionTypes(feed).contains("ASSIGNEE_UPDATED"), "исполнителя не меняли");
        assertFalse(actionTypes(feed).contains("STATUS_CHANGED"), "статус не меняли");

        Object fields = details(firstOfType(feed, "TASK_UPDATED")).get("fields");
        assertTrue(fields instanceof List, "ожидался список изменённых полей");
        assertTrue(((List<?>) fields).containsAll(List.of("title", "estimatedHours")), String.valueOf(fields));
    }

    @Test
    public void writingASummaryIsLogged() {
        auth(ownerToken).body("{\"summary\": \"вывод модели\"}")
            .put("/api/tasks/" + taskId + "/summary").then().statusCode(200);

        Map<String, Object> event = firstOfType(projectFeed(ownerToken), "SUMMARY_UPDATED");
        assertEquals(taskId, event.get("taskId"));
        assertEquals(12, details(event).get("length"));
    }

    @Test
    public void discussionWritesAreLogged() {
        auth(ownerToken).body("{\"author\": \"claude\", \"type\": \"DECISION\", \"content\": \"решение\"}")
            .post("/api/tasks/" + taskId + "/discussion").then().statusCode(201);

        Map<String, Object> added = firstOfType(projectFeed(ownerToken), "DISCUSSION_BLOCK_ADDED");
        assertEquals("claude", details(added).get("author"));
        assertEquals("DECISION", details(added).get("type"));

        auth(ownerToken).body("[{\"content\": \"переписали\"}]")
            .put("/api/tasks/" + taskId + "/discussion").then().statusCode(200);

        Map<String, Object> replaced = firstOfType(projectFeed(ownerToken), "DISCUSSION_REPLACED");
        assertEquals(1, details(replaced).get("blocks"));
    }

    @Test
    public void membershipChangesAreLogged() {
        addMember(memberId, ROLE_DEVELOPER);

        auth(ownerToken).body("{\"roleId\": " + ROLE_MANAGER + "}")
            .put("/api/projects/" + projectId + "/members/" + memberId).then().statusCode(200);

        auth(ownerToken).delete("/api/projects/" + projectId + "/members/" + memberId).then().statusCode(204);

        List<Map<String, Object>> feed = projectFeed(ownerToken);
        assertTrue(actionTypes(feed).containsAll(
            List.of("MEMBER_ADDED", "MEMBER_ROLE_CHANGED", "MEMBER_REMOVED")), String.valueOf(actionTypes(feed)));

        Map<String, Object> roleChanged = firstOfType(feed, "MEMBER_ROLE_CHANGED");
        assertEquals(ROLE_DEVELOPER, details(roleChanged).get("fromRoleId"));
        assertEquals(ROLE_MANAGER, details(roleChanged).get("toRoleId"));
        assertEquals("Manager", details(roleChanged).get("roleName"));
    }

    @Test
    public void feedIsNewestFirst() {
        auth(ownerToken).body("{\"title\": \"после всего\"}").patch(taskPath()).then().statusCode(200);

        List<Map<String, Object>> feed = projectFeed(ownerToken);
        assertEquals("TASK_UPDATED", feed.get(0).get("actionType"));
        assertEquals("PROJECT_CREATED", feed.get(feed.size() - 1).get("actionType"));
    }

    @Test
    public void limitAndOffsetPageTheFeed() {
        List<Map<String, Object>> whole = projectFeed(ownerToken);
        assertTrue(whole.size() >= 3, "нужно хотя бы три события");

        List<Map<String, Object>> firstPage = feed(ownerToken, projectActivityPath() + "?limit=2");
        assertEquals(2, firstPage.size());
        assertEquals(whole.get(0).get("id"), firstPage.get(0).get("id"));

        List<Map<String, Object>> secondPage = feed(ownerToken, projectActivityPath() + "?limit=2&offset=2");
        assertEquals(whole.get(2).get("id"), secondPage.get(0).get("id"));
    }

    @Test
    public void invalidPagingIsRejected() {
        auth(ownerToken).get(projectActivityPath() + "?limit=0").then().statusCode(400);
        auth(ownerToken).get(projectActivityPath() + "?offset=-1").then().statusCode(400);
        auth(ownerToken).get(projectActivityPath() + "?limit=100000").then().statusCode(200);
    }

    @Test
    public void taskFeedShowsOnlyThatTasksEvents() {
        Integer otherTaskId = auth(ownerToken).body("{\"title\": \"вторая задача\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body("{\"title\": \"переименована\"}").patch(taskPath()).then().statusCode(200);

        List<Map<String, Object>> taskFeed = feed(ownerToken, "/api/tasks/" + taskId + "/activity");
        assertFalse(taskFeed.isEmpty());
        taskFeed.forEach(event -> assertEquals(taskId, event.get("taskId")));

        List<Map<String, Object>> otherFeed = feed(ownerToken, "/api/tasks/" + otherTaskId + "/activity");
        assertEquals(1, otherFeed.size(), "у второй задачи только её создание");
        assertEquals("TASK_CREATED", otherFeed.get(0).get("actionType"));
    }

    @Test
    public void memberSeesTheFeedAndStrangerDoesNot() {
        addMember(memberId, ROLE_DEVELOPER);

        auth(memberToken).get(projectActivityPath()).then().statusCode(200);
        auth(strangerToken).get(projectActivityPath()).then().statusCode(403);
        auth(strangerToken).get("/api/tasks/" + taskId + "/activity").then().statusCode(403);
    }

    @Test
    public void unknownProjectOrTaskGives404() {
        auth(ownerToken).get("/api/projects/99999/activity").then().statusCode(404);
        auth(ownerToken).get("/api/tasks/99999/activity").then().statusCode(404);
    }

    @Test
    public void seededLegacyEntriesStayReadable() {
        // В сиде лежат записи с типами из старого вокабуляра; чтение не должно на них ломаться.
        membershipRepository.save(ProjectMembership.create(SEEDED_PROJECT_ID, ownerId, ROLE_DEVELOPER));

        auth(ownerToken).get("/api/projects/" + SEEDED_PROJECT_ID + "/activity").then()
            .statusCode(200)
            .body("$", hasSize(4))
            .body("actionType", hasItems("TASK_CREATED", "ASSIGNEE_UPDATED", "STATUS_CHANGED", "COMMENT_ADDED"));
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).get(projectActivityPath()).then().statusCode(401);
    }

    private void addMember(Integer userId, int roleId) {
        auth(ownerToken).body("{\"userId\": " + userId + ", \"roleId\": " + roleId + "}")
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
    }

    private List<Map<String, Object>> projectFeed(String token) {
        return feed(token, projectActivityPath());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> feed(String token, String path) {
        return auth(token).get(path).then().statusCode(200)
            .extract().jsonPath().getList("$", Map.class).stream()
            .map(entry -> (Map<String, Object>) entry)
            .toList();
    }

    private static List<String> actionTypes(List<Map<String, Object>> feed) {
        return feed.stream().map(event -> (String) event.get("actionType")).toList();
    }

    private static Map<String, Object> firstOfType(List<Map<String, Object>> feed, String actionType) {
        return feed.stream()
            .filter(event -> actionType.equals(event.get("actionType")))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "В ленте нет события " + actionType + ", есть: " + actionTypes(feed)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> details(Map<String, Object> event) {
        Object details = event.get("details");
        assertNotNull(details, "у события " + event.get("actionType") + " нет подробностей");
        return (Map<String, Object>) details;
    }

    private String taskPath() {
        return "/api/projects/" + projectId + "/tasks/" + taskId;
    }

    private String projectActivityPath() {
        return "/api/projects/" + projectId + "/activity";
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
