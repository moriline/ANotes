package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.CommentRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.domain.model.Visibility;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class CommentResourceTest {

    @Inject TestDataCleanup cleanup;

    private String token;
    private Integer taskId;

    /** Свой проект и задача: создатель попадает в участники автоматически, доступ есть. */
    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        token = TestAuthHelper.registerAndLogin("comment-" + ts, "comment-" + ts + "@test.com", "Pass123!");

        Integer projectId = auth().body(new ProjectRequest("comment-project-" + ts, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
        taskId = auth().body(new TaskRequest("comment task", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void commentLifecycle() {
        Integer commentId = auth()
            .body(new CommentRequest("Initial comment", null))
            .post("/api/tasks/" + taskId + "/comments")
            .then().statusCode(201)
            .extract().path("id");

        auth().body(new CommentRequest("Updated comment", null))
            .put("/api/comments/" + commentId)
            .then().statusCode(200)
            .body("content", is("Updated comment"))
            .body("isEdited", is(true));

        auth().delete("/api/comments/" + commentId).then().statusCode(204);

        auth().get("/api/tasks/" + taskId + "/comments")
            .then().statusCode(200)
            .body("id", not(hasItem(commentId)));
    }

    @Test
    public void visibilityDefaultsToPublicAndRoundTrips() {
        // Без поля в запросе — PUBLIC.
        auth().body(new CommentRequest("no visibility given", null))
            .post("/api/tasks/" + taskId + "/comments")
            .then().statusCode(201)
            .body("visibility", is("PUBLIC"));

        // Явный INTERNAL сохраняется.
        Integer internalId = auth()
            .body(new CommentRequest("budget note", Visibility.INTERNAL))
            .post("/api/tasks/" + taskId + "/comments")
            .then().statusCode(201)
            .body("visibility", is("INTERNAL"))
            .extract().path("id");

        // Правка текста без visibility в запросе не сбрасывает INTERNAL на PUBLIC.
        auth().body(new CommentRequest("budget note (fixed)", null))
            .put("/api/comments/" + internalId)
            .then().statusCode(200)
            .body("visibility", is("INTERNAL"));
    }

    private RequestSpecification auth() {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
