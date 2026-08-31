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
 * Карточки по прямой ссылке: {@code GET /api/projects/{id}} и
 * {@code GET /api/projects/{projectId}/tasks/{taskId}}. Раньше открыть проект
 * или задачу по её id было нечем — только выгружать списки.
 */
@QuarkusTest
public class ProjectAndTaskFetchTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer projectId;
    private Integer taskId;
    private Integer otherProjectId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("get-owner-" + ts, "get-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("get-member-" + ts, "get-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("get-stranger-" + ts, "get-stranger-" + ts + "@test.com", "Pass123!");

        projectId = createProject(ownerToken, "fetch-project", "проект для карточек");
        otherProjectId = createProject(strangerToken, "fetch-foreign-project", "чужой проект");

        auth(ownerToken).body("{\"userId\": " + meId(memberToken) + ", \"roleId\": " + ROLE_DEVELOPER + "}")
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);

        taskId = auth(ownerToken).body("{\"title\": \"Карточка задачи\", \"description\": \"описание\", \"tags\": [\"ui\"]}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void ownerOpensProjectCard() {
        auth(ownerToken).get("/api/projects/" + projectId).then()
            .statusCode(200)
            .body("id", equalTo(projectId))
            .body("name", equalTo("fetch-project"))
            .body("description", equalTo("проект для карточек"))
            .body("isActive", equalTo(true));
    }

    @Test
    public void memberOpensProjectCard() {
        auth(memberToken).get("/api/projects/" + projectId).then()
            .statusCode(200)
            .body("id", equalTo(projectId));
    }

    @Test
    public void strangerCannotOpenForeignProjectCard() {
        auth(strangerToken).get("/api/projects/" + projectId).then().statusCode(403);
    }

    @Test
    public void unknownProjectGives404() {
        auth(ownerToken).get("/api/projects/99999").then().statusCode(404);
    }

    @Test
    public void ownerOpensTaskCardWithAllItsFields() {
        auth(ownerToken).body("{\"summary\": \"итог задачи\"}")
            .put("/api/tasks/" + taskId + "/summary").then().statusCode(200);

        auth(ownerToken).get("/api/projects/" + projectId + "/tasks/" + taskId).then()
            .statusCode(200)
            .body("id", equalTo(taskId))
            .body("projectId", equalTo(projectId))
            .body("title", equalTo("Карточка задачи"))
            .body("description", equalTo("описание"))
            .body("summary", equalTo("итог задачи"))
            .body("tags", hasItem("ui"))
            .body("statusId", notNullValue());
    }

    @Test
    public void memberOpensTaskCard() {
        auth(memberToken).get("/api/projects/" + projectId + "/tasks/" + taskId).then()
            .statusCode(200)
            .body("id", equalTo(taskId));
    }

    @Test
    public void taskLookedUpUnderTheWrongProjectGives404() {
        auth(strangerToken).get("/api/projects/" + otherProjectId + "/tasks/" + taskId).then().statusCode(404);
    }

    @Test
    public void unknownTaskGives404() {
        auth(ownerToken).get("/api/projects/" + projectId + "/tasks/99999").then().statusCode(404);
    }

    @Test
    public void strangerCannotOpenTaskCard() {
        auth(strangerToken).get("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(403);
    }

    @Test
    public void taskListOfAForeignProjectIsNoLongerReadable() {
        auth(ownerToken).get("/api/projects/" + projectId + "/tasks").then()
            .statusCode(200)
            .body("id", hasItem(taskId));

        auth(strangerToken).get("/api/projects/" + projectId + "/tasks").then().statusCode(403);
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).get("/api/projects/" + projectId).then().statusCode(401);
        given().contentType(ContentType.JSON)
            .get("/api/projects/" + projectId + "/tasks/" + taskId).then().statusCode(401);
    }

    private Integer createProject(String token, String name, String description) {
        return auth(token).body("{\"name\": \"" + name + "\", \"description\": \"" + description + "\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
