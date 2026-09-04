package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskUpdateRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Сортировка и постраничность в /api/find. Раньше выдача шла в произвольном
 * порядке и целиком, без limit/offset и без total.
 */
@QuarkusTest
public class FindSortingPagingTest {

    @Inject TestDataCleanup cleanup;

    private String token;
    private Integer projectId;
    private Integer alphaId;
    private Integer bravoId;
    private Integer charlieId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        token = TestAuthHelper.registerAndLogin("sort-user-" + ts, "sort-user-" + ts + "@test.com", "Pass123!");

        projectId = auth().body(new ProjectRequest("sorting-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        alphaId = createTask("alpha");
        bravoId = createTask("bravo");
        charlieId = createTask("charlie");
    }

    @Test
    public void newestUpdatedComesFirstByDefault() {
        assertEquals(List.of(charlieId, bravoId, alphaId), idsOf(find(new FindTasksRequest())));

        auth().body(new TaskUpdateRequest(null, "тронули alpha", null, null, null, null, null, null, null))
            .patch("/api/projects/" + projectId + "/tasks/" + alphaId).then().statusCode(200);

        assertEquals(alphaId, idsOf(find(new FindTasksRequest())).get(0), "изменённая задача должна всплыть наверх");
    }

    @Test
    public void sortsByTitleInBothDirections() {
        assertEquals(List.of("alpha", "bravo", "charlie"), titlesOf(find(sorted("title", "asc"))));
        assertEquals(List.of("charlie", "bravo", "alpha"), titlesOf(find(sorted("title", "desc"))));
    }

    @Test
    public void sortsByCreationTime() {
        assertEquals(List.of(alphaId, bravoId, charlieId), idsOf(find(sorted("createdAt", "asc"))));
    }

    @Test
    public void sortsByDueDate() {
        setDueDate(alphaId, 3_000_000_000_000L);
        setDueDate(bravoId, 1_000_000_000_000L);
        setDueDate(charlieId, 2_000_000_000_000L);

        assertEquals(List.of(bravoId, charlieId, alphaId), idsOf(find(sorted("dueDate", "asc"))));
    }

    @Test
    public void sortFieldNameIsAcceptedInBothSpellings() {
        assertEquals(titlesOf(find(sorted("dueDate", "asc"))),
            titlesOf(find(sorted("DUE_DATE", "ASC"))));
    }

    @Test
    public void limitAndOffsetPageThroughTheResult() {
        FindTasksRequest page1 = sorted("title", "asc");
        page1.limit = 2;
        JsonPath firstPage = find(page1);
        assertEquals(List.of("alpha", "bravo"), titlesOf(firstPage));
        assertEquals(3, firstPage.getLong("total"), "total считает всё, а не страницу");
        assertEquals(2, firstPage.getInt("limit"));
        assertEquals(0, firstPage.getInt("offset"));

        FindTasksRequest page2 = sorted("title", "asc");
        page2.limit = 2;
        page2.offset = 2;
        JsonPath secondPage = find(page2);
        assertEquals(List.of("charlie"), titlesOf(secondPage));
        assertEquals(3, secondPage.getLong("total"));
        assertEquals(2, secondPage.getInt("offset"));

        FindTasksRequest pastEnd = new FindTasksRequest();
        pastEnd.limit = 2;
        pastEnd.offset = 99;
        assertEquals(List.of(), titlesOf(find(pastEnd)));
    }

    @Test
    public void totalCountsWhatMatchesTheFilterNotThePage() {
        FindTasksRequest filteredQuery = new FindTasksRequest();
        filteredQuery.titleSearch = "alpha";
        filteredQuery.limit = 1;
        JsonPath filtered = find(filteredQuery);
        assertEquals(1, filtered.getLong("total"));

        FindTasksRequest everythingQuery = new FindTasksRequest();
        everythingQuery.limit = 1;
        JsonPath everything = find(everythingQuery);
        assertEquals(3, everything.getLong("total"));
        assertEquals(1, everything.getList("tasks").size());
    }

    @Test
    public void unknownSortFieldOrDirectionIsRejected() {
        assertRejected(sorted("password", null));
        assertRejected(sorted("title; DROP TABLE tasks", null));
        assertRejected(sorted(null, "sideways"));
    }

    @Test
    public void invalidPagingIsRejectedAndHugeLimitIsClamped() {
        FindTasksRequest zeroLimit = new FindTasksRequest();
        zeroLimit.limit = 0;
        assertRejected(zeroLimit);

        FindTasksRequest negativeOffset = new FindTasksRequest();
        negativeOffset.offset = -1;
        assertRejected(negativeOffset);

        FindTasksRequest hugeLimit = new FindTasksRequest();
        hugeLimit.limit = 100_000;
        auth().body(hugeLimit).post("/api/find").then()
            .statusCode(200)
            .body("limit", equalTo(200));
    }

    @Test
    public void emptyScopeStillReportsPagingFields() {
        long ts = System.nanoTime();
        String strangerToken = TestAuthHelper.registerAndLogin(
            "sort-stranger-" + ts, "sort-stranger-" + ts + "@test.com", "Pass123!");

        FindTasksRequest query = new FindTasksRequest();
        query.limit = 5;
        TestAuthHelper.authenticated(strangerToken).body(query)
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks", hasSize(0))
            .body("total", equalTo(0))
            .body("limit", equalTo(5));
    }

    private void setDueDate(Integer taskId, long dueDate) {
        auth().body(new TaskUpdateRequest(null, null, null, null, null, dueDate, null, null, null))
            .patch("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(200);
    }

    /** {@code sortBy}/{@code sortDir} — самая частая пара фильтров в этих тестах. */
    private static FindTasksRequest sorted(String sortBy, String sortDir) {
        FindTasksRequest query = new FindTasksRequest();
        query.sortBy = sortBy;
        query.sortDir = sortDir;
        return query;
    }

    private void assertRejected(FindTasksRequest body) {
        auth().body(body).post("/api/find").then().statusCode(400);
    }

    private JsonPath find(FindTasksRequest body) {
        return auth().body(body).post("/api/find").then().statusCode(200).extract().jsonPath();
    }

    private static List<Integer> idsOf(JsonPath response) {
        return response.getList("tasks.id", Integer.class);
    }

    private static List<String> titlesOf(JsonPath response) {
        return response.getList("tasks.title", String.class);
    }

    private Integer createTask(String title) {
        return auth().body(new TaskRequest(title, null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private RequestSpecification auth() {
        return TestAuthHelper.authenticated(token);
    }
}
