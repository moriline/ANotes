package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentResourceTest {

    @Inject TestDataCleanup cleanup;

    private static Integer projectId;
    private static Integer taskId;
    private static Integer commentId;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("commentuser-" + System.currentTimeMillis(), "comment@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void prepareData() {
        // Create Project
        Response projResp = authenticated()
            .body("{\"name\": \"comment-project\"}")
        .when().post("/api/projects")
        .then().statusCode(201).extract().response();
        projectId = projResp.jsonPath().getInt("id");

        // Create Task
        Response taskResp = authenticated()
            .body("{\"title\": \"Task for comments\"}")
        .when().post("/api/projects/" + projectId + "/tasks")
        .then().statusCode(201).extract().response();
        taskId = taskResp.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldAddComment() {
        Response response = authenticated()
            .body("{\"content\": \"This is a test comment\"}")
        .when().post("/api/tasks/{taskId}/comments", taskId)
        .then()
            .statusCode(201)
            .body("content", equalTo("This is a test comment"))
            .body("taskId", equalTo(taskId))
            .body("userId", notNullValue())
            .body("isEdited", equalTo(false))
        .extract().response();

        commentId = response.jsonPath().getInt("id");
    }

    @Test
    @Order(3)
    void shouldListComments() {
        authenticated()
        .when().get("/api/tasks/{taskId}/comments", taskId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("content", hasItem("This is a test comment"));
    }

    @Test
    @Order(4)
    void shouldUpdateComment() {
        authenticated()
            .body("{\"content\": \"Updated comment content\"}")
        .when().put("/api/comments/{commentId}", commentId)
        .then()
            .statusCode(200)
            .body("content", equalTo("Updated comment content"))
            .body("isEdited", equalTo(true));
    }

    @Test
    @Order(5)
    void shouldDeleteComment() {
        authenticated()
        .when().delete("/api/comments/{commentId}", commentId)
        .then()
            .statusCode(204);

        authenticated()
        .when().get("/api/tasks/{taskId}/comments", taskId)
        .then()
            .body("id", not(hasItem(commentId)));
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
