package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
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

        projectId = auth().body("{\"name\": \"sorting-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        alphaId = createTask("alpha");
        bravoId = createTask("bravo");
        charlieId = createTask("charlie");
    }

    @Test
    public void newestUpdatedComesFirstByDefault() {
        assertEquals(List.of(charlieId, bravoId, alphaId), idsOf(find("{}")));

        auth().body("{\"description\": \"тронули alpha\"}")
            .patch("/api/projects/" + projectId + "/tasks/" + alphaId).then().statusCode(200);

        assertEquals(alphaId, idsOf(find("{}")).get(0), "изменённая задача должна всплыть наверх");
    }

    @Test
    public void sortsByTitleInBothDirections() {
        assertEquals(List.of("alpha", "bravo", "charlie"),
            titlesOf(find("{\"sortBy\": \"title\", \"sortDir\": \"asc\"}")));

        assertEquals(List.of("charlie", "bravo", "alpha"),
            titlesOf(find("{\"sortBy\": \"title\", \"sortDir\": \"desc\"}")));
    }

    @Test
    public void sortsByCreationTime() {
        assertEquals(List.of(alphaId, bravoId, charlieId),
            idsOf(find("{\"sortBy\": \"createdAt\", \"sortDir\": \"asc\"}")));
    }

    @Test
    public void sortsByDueDate() {
        setDueDate(alphaId, 3_000_000_000_000L);
        setDueDate(bravoId, 1_000_000_000_000L);
        setDueDate(charlieId, 2_000_000_000_000L);

        assertEquals(List.of(bravoId, charlieId, alphaId),
            idsOf(find("{\"sortBy\": \"dueDate\", \"sortDir\": \"asc\"}")));
    }

    @Test
    public void sortFieldNameIsAcceptedInBothSpellings() {
        assertEquals(titlesOf(find("{\"sortBy\": \"dueDate\", \"sortDir\": \"asc\"}")),
            titlesOf(find("{\"sortBy\": \"DUE_DATE\", \"sortDir\": \"ASC\"}")));
    }

    @Test
    public void limitAndOffsetPageThroughTheResult() {
        JsonPath firstPage = find("{\"sortBy\": \"title\", \"sortDir\": \"asc\", \"limit\": 2}");
        assertEquals(List.of("alpha", "bravo"), titlesOf(firstPage));
        assertEquals(3, firstPage.getLong("total"), "total считает всё, а не страницу");
        assertEquals(2, firstPage.getInt("limit"));
        assertEquals(0, firstPage.getInt("offset"));

        JsonPath secondPage = find("{\"sortBy\": \"title\", \"sortDir\": \"asc\", \"limit\": 2, \"offset\": 2}");
        assertEquals(List.of("charlie"), titlesOf(secondPage));
        assertEquals(3, secondPage.getLong("total"));
        assertEquals(2, secondPage.getInt("offset"));

        assertEquals(List.of(), titlesOf(find("{\"limit\": 2, \"offset\": 99}")));
    }

    @Test
    public void totalCountsWhatMatchesTheFilterNotThePage() {
        JsonPath filtered = find("{\"titleSearch\": \"alpha\", \"limit\": 1}");
        assertEquals(1, filtered.getLong("total"));

        JsonPath everything = find("{\"limit\": 1}");
        assertEquals(3, everything.getLong("total"));
        assertEquals(1, everything.getList("tasks").size());
    }

    @Test
    public void unknownSortFieldOrDirectionIsRejected() {
        auth().body("{\"sortBy\": \"password\"}").post("/api/find").then().statusCode(400);
        auth().body("{\"sortBy\": \"title; DROP TABLE tasks\"}").post("/api/find").then().statusCode(400);
        auth().body("{\"sortDir\": \"sideways\"}").post("/api/find").then().statusCode(400);
    }

    @Test
    public void invalidPagingIsRejectedAndHugeLimitIsClamped() {
        auth().body("{\"limit\": 0}").post("/api/find").then().statusCode(400);
        auth().body("{\"offset\": -1}").post("/api/find").then().statusCode(400);

        auth().body("{\"limit\": 100000}").post("/api/find").then()
            .statusCode(200)
            .body("limit", equalTo(200));
    }

    @Test
    public void emptyScopeStillReportsPagingFields() {
        long ts = System.nanoTime();
        String strangerToken = TestAuthHelper.registerAndLogin(
            "sort-stranger-" + ts, "sort-stranger-" + ts + "@test.com", "Pass123!");

        TestAuthHelper.authenticated(strangerToken).body("{\"limit\": 5}")
            .post("/api/find").then()
            .statusCode(200)
            .body("tasks", hasSize(0))
            .body("total", equalTo(0))
            .body("limit", equalTo(5));
    }

    private void setDueDate(Integer taskId, long dueDate) {
        auth().body("{\"dueDate\": " + dueDate + "}")
            .patch("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(200);
    }

    private JsonPath find(String body) {
        return auth().body(body).post("/api/find").then().statusCode(200).extract().jsonPath();
    }

    private static List<Integer> idsOf(JsonPath response) {
        return response.getList("tasks.id", Integer.class);
    }

    private static List<String> titlesOf(JsonPath response) {
        return response.getList("tasks.title", String.class);
    }

    private Integer createTask(String title) {
        return auth().body("{\"title\": \"" + title + "\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private RequestSpecification auth() {
        return TestAuthHelper.authenticated(token);
    }
}
