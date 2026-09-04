package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * {@code /api/tasks/{taskId}/time}: время списывается на задачу проекта, к
 * которому у вызывающего есть доступ; автор берётся из токена, а не из тела.
 */
@QuarkusTest
public class TimeEntryResourceTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private int ownerTaskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("time-owner-" + ts, "time-owner-" + ts + "@test.com", "Pass123!");
        ownerTaskId = createTask(ownerToken, createProject(ownerToken, "time-proj-" + ts));
    }

    @Test
    public void shouldLogTimeAndGetTotalTime() {
        auth(ownerToken)
            .body("{\"seconds\": 3600, \"description\": \"Test work\"}")
            .post("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(200).body("seconds", is(3600));

        auth(ownerToken)
            .get("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(200).body(is("3600"));
    }

    @Test
    public void shouldAttributeEntryToCallerNotToUserIdFromBody() {
        int callerId = meId(ownerToken);

        // Тело намеренно пытается подставить чужого автора — userId=1 (admin из сида).
        auth(ownerToken)
            .body("{\"userId\": 1, \"seconds\": 1200, \"description\": \"Чужое время\"}")
            .post("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(200)
            .body("userId", equalTo(callerId))
            .body("userId", not(equalTo(1)));
    }

    @Test
    public void shouldKeepEntriesOfDifferentUsersSeparateButSumThemTogether() {
        long ts = System.nanoTime();
        String memberToken = TestAuthHelper.registerAndLogin("time-b-" + ts, "time-b-" + ts + "@test.com", "Pass123!");
        int memberId = meId(memberToken);

        Integer project = createProject(ownerToken, "time-shared-" + ts);
        int taskId = createTask(ownerToken, project);
        auth(ownerToken).body("{\"userId\": " + memberId + ", \"roleId\": " + ROLE_DEVELOPER + "}")
            .post("/api/projects/" + project + "/members").then().statusCode(201);

        auth(ownerToken).body("{\"seconds\": 1800, \"description\": \"A\"}")
            .post("/api/tasks/" + taskId + "/time").then().statusCode(200).body("userId", equalTo(meId(ownerToken)));
        auth(memberToken).body("{\"seconds\": 1800, \"description\": \"B\"}")
            .post("/api/tasks/" + taskId + "/time").then().statusCode(200).body("userId", equalTo(memberId));

        auth(ownerToken).get("/api/tasks/" + taskId + "/time").then().statusCode(200).body(is("3600"));
    }

    @Test
    public void shouldRejectRequestWithoutSeconds() {
        auth(ownerToken)
            .body("{\"description\": \"без часов\"}")
            .post("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(400);
    }

    @Test
    public void shouldRejectUnauthenticated() {
        given().contentType(ContentType.JSON)
            .body("{\"seconds\": 600}")
            .post("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(401);
    }

    @Test
    public void strangerCannotLogOrReadTimeOnAForeignTask() {
        long ts = System.nanoTime();
        String strangerToken = TestAuthHelper.registerAndLogin("time-stranger-" + ts, "time-stranger-" + ts + "@test.com", "Pass123!");

        auth(strangerToken)
            .body("{\"seconds\": 600}")
            .post("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(403);

        auth(strangerToken)
            .get("/api/tasks/" + ownerTaskId + "/time")
            .then().statusCode(403);
    }

    @Test
    public void loggingTimeOnAMissingTaskIs404() {
        auth(ownerToken)
            .body("{\"seconds\": 600}")
            .post("/api/tasks/999999/time")
            .then().statusCode(404);
    }

    // --- helpers ---------------------------------------------------------

    private Integer createProject(String token, String name) {
        return auth(token).body("{\"name\": \"" + name + "\"}")
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");
    }

    private int createTask(String token, Integer projectId) {
        return auth(token).body("{\"title\": \"task\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
