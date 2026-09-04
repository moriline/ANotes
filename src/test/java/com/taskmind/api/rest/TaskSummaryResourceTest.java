package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskSummaryRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Итог по задаче: {@code GET/PUT /api/tasks/{taskId}/summary}. Это место, куда
 * модель кладёт вывод, решив задачу.
 */
@QuarkusTest
public class TaskSummaryResourceTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer projectId;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("sum-owner-" + ts, "sum-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("sum-member-" + ts, "sum-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("sum-stranger-" + ts, "sum-stranger-" + ts + "@test.com", "Pass123!");

        projectId = auth(ownerToken).body(new ProjectRequest("summary-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body(new ProjectMemberRequest(meId(memberToken), ROLE_DEVELOPER))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);

        taskId = auth(ownerToken).body(new TaskRequest("Разобраться с кешем", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void summaryIsEmptyUntilSomebodyWritesIt() {
        auth(ownerToken).get(summaryPath()).then()
            .statusCode(200)
            .body("taskId", equalTo(taskId))
            .body("summary", nullValue());
    }

    @Test
    public void putStoresTheConclusionAndGetReturnsIt() {
        String conclusion = "Причина в TTL кеша: он сбрасывался при каждом деплое.";

        auth(ownerToken).body(summaryBody(conclusion))
            .put(summaryPath()).then()
            .statusCode(200)
            .body("taskId", equalTo(taskId))
            .body("summary", equalTo(conclusion))
            .body("updatedAt", notNullValue());

        auth(ownerToken).get(summaryPath()).then()
            .statusCode(200)
            .body("summary", equalTo(conclusion));
    }

    @Test
    public void putReplacesThePreviousConclusion() {
        auth(ownerToken).body(summaryBody("первая версия")).put(summaryPath()).then().statusCode(200);
        auth(ownerToken).body(summaryBody("уточнённая версия")).put(summaryPath()).then()
            .statusCode(200)
            .body("summary", equalTo("уточнённая версия"));
    }

    @Test
    public void emptyStringClearsTheConclusion() {
        auth(ownerToken).body(summaryBody("было")).put(summaryPath()).then().statusCode(200);

        auth(ownerToken).body(new TaskSummaryRequest("")).put(summaryPath()).then()
            .statusCode(200)
            .body("summary", equalTo(""));
    }

    @Test
    public void conclusionIsVisibleInTheTaskItself() {
        auth(ownerToken).body(summaryBody("итог виден в задаче")).put(summaryPath()).then().statusCode(200);

        auth(ownerToken).get("/api/projects/" + projectId + "/tasks").then()
            .statusCode(200)
            .body("find { it.id == " + taskId + " }.summary", equalTo("итог виден в задаче"));

        auth(ownerToken).body(new FindTasksRequest()).post("/api/find").then()
            .statusCode(200)
            .body("tasks.find { it.id == " + taskId + " }.summary", equalTo("итог виден в задаче"));
    }

    @Test
    public void memberOfTheProjectCanWriteTheConclusion() {
        auth(memberToken).body(summaryBody("вывод от участника")).put(summaryPath()).then()
            .statusCode(200)
            .body("summary", equalTo("вывод от участника"));
    }

    @Test
    public void requestWithoutSummaryFieldIsRejected() {
        auth(ownerToken).body(new TaskSummaryRequest(null)).put(summaryPath()).then().statusCode(400);
    }

    @Test
    public void strangerCanNeitherReadNorWrite() {
        auth(strangerToken).get(summaryPath()).then().statusCode(403);
        auth(strangerToken).body(summaryBody("чужое")).put(summaryPath()).then().statusCode(403);
    }

    @Test
    public void unknownTaskGives404() {
        auth(ownerToken).get("/api/tasks/99999/summary").then().statusCode(404);
        auth(ownerToken).body(summaryBody("нет такой")).put("/api/tasks/99999/summary").then().statusCode(404);
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).get(summaryPath()).then().statusCode(401);
    }

    private String summaryPath() {
        return "/api/tasks/" + taskId + "/summary";
    }

    private static TaskSummaryRequest summaryBody(String summary) {
        return new TaskSummaryRequest(summary);
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
