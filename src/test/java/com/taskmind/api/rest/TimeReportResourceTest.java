package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.LoginRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TimeEntryRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * {@code GET /api/reports/time}: свой отчёт, отчёт коллеги (только общие проекты),
 * отчёт админа (всё), и фильтры year/month по {@code startTime} записи.
 */
@QuarkusTest
public class TimeReportResourceTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer ownerId;
    private Integer memberId;
    private Integer sharedTaskId;
    private Integer privateTaskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("tr-owner-" + ts, "tr-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("tr-member-" + ts, "tr-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("tr-stranger-" + ts, "tr-stranger-" + ts + "@test.com", "Pass123!");
        ownerId = meId(ownerToken);
        memberId = meId(memberToken);

        Integer sharedProject = createProject("tr-shared-" + ts);
        Integer privateProject = createProject("tr-private-" + ts);
        sharedTaskId = createTask(sharedProject);
        privateTaskId = createTask(privateProject);

        auth(ownerToken).body(new ProjectMemberRequest(memberId, ROLE_DEVELOPER))
            .post("/api/projects/" + sharedProject + "/members").then().statusCode(201);

        // owner: 2000с на общем проекте и 5000с на своём закрытом — обе записи в июне 2025.
        logTime(ownerToken, sharedTaskId, 2000, utcMillis(2025, 6, 10));
        logTime(ownerToken, privateTaskId, 5000, utcMillis(2025, 6, 12));
        // member: 1500с на общем проекте в июне, 600с в августе.
        logTime(memberToken, sharedTaskId, 1500, utcMillis(2025, 6, 15));
        logTime(memberToken, sharedTaskId, 600, utcMillis(2025, 8, 1));
    }

    @Test
    public void ownMonthlyAndYearlyReport() {
        auth(memberToken).get("/api/reports/time?year=2025&month=6").then()
            .statusCode(200)
            .body("userId", is(memberId))
            .body("month", is(6))
            .body("totalSeconds", is(1500))
            .body("entryCount", is(1))
            .body("byProject.size()", is(1));

        auth(memberToken).get("/api/reports/time?year=2025").then()
            .statusCode(200)
            .body("month", nullValue())
            .body("totalSeconds", is(2100));

        auth(memberToken).get("/api/reports/time?year=2025&month=7").then()
            .statusCode(200).body("totalSeconds", is(0)).body("byProject", empty());
    }

    @Test
    public void defaultsToSelfAndCurrentYear() {
        long now = System.currentTimeMillis();
        logTime(memberToken, sharedTaskId, 300, now);

        auth(memberToken).get("/api/reports/time").then()
            .statusCode(200)
            .body("userId", is(memberId))
            .body("year", is(Year.now(ZoneOffset.UTC).getValue()))
            .body("totalSeconds", is(300));
    }

    @Test
    public void coworkerSeesOnlySharedProjectTime() {
        // member смотрит отчёт owner: 5000с закрытого проекта не видит, 2000с общего — да.
        auth(memberToken).get("/api/reports/time?userId=" + ownerId + "&year=2025&month=6").then()
            .statusCode(200)
            .body("userId", is(ownerId))
            .body("totalSeconds", is(2000))
            .body("byProject.size()", is(1))
            .body("byProject[0].projectName", startsWith("tr-shared-"));
    }

    @Test
    public void adminSeesEveryProject() {
        String adminToken = login("admin", "admin123");

        auth(adminToken).get("/api/reports/time?userId=" + ownerId + "&year=2025&month=6").then()
            .statusCode(200)
            .body("totalSeconds", is(7000))
            .body("byProject.size()", is(2));
    }

    @Test
    public void selfReportIncludesProjectsTheUserOnlyOwns() {
        // owner не «участник» закрытого проекта в смысле projectMembers, но это его проект
        // и его время — свой отчёт показывает всё.
        auth(ownerToken).get("/api/reports/time?year=2025&month=6").then()
            .statusCode(200)
            .body("totalSeconds", is(7000))
            .body("byProject.size()", is(2));
    }

    @Test
    public void strangerWithoutSharedProjectIsForbidden() {
        auth(strangerToken).get("/api/reports/time?userId=" + ownerId).then().statusCode(403);
    }

    @Test
    public void badMonthOrUnknownUserAreRejected() {
        auth(memberToken).get("/api/reports/time?year=2025&month=13").then().statusCode(400);
        auth(memberToken).get("/api/reports/time?year=2025&month=0").then().statusCode(400);
        auth(memberToken).get("/api/reports/time?userId=999999").then().statusCode(404);
    }

    // --- helpers ---------------------------------------------------------

    private Integer createProject(String name) {
        return auth(ownerToken).body(new ProjectRequest(name, null))
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");
    }

    private Integer createTask(Integer projectId) {
        return auth(ownerToken).body(new TaskRequest("task", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private void logTime(String token, Integer taskId, long seconds, long startTime) {
        auth(token).body(new TimeEntryRequest(seconds, null, startTime))
            .post("/api/tasks/" + taskId + "/time").then().statusCode(200);
    }

    private static long utcMillis(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
    }

    private static String login(String username, String password) {
        return given().contentType(ContentType.JSON)
            .body(new LoginRequest(username, password))
            .post("/api/auth/login").then().statusCode(200).extract().jsonPath().getString("token");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
