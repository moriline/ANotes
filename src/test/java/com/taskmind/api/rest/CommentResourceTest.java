package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class CommentResourceTest {

    @Test
    public void testCommentLifecycle() {
        String token = TestAuthHelper.registerAndLogin("comment_user", "comment@test.com", "password");

        // 1. Создать комментарий (POST /api/tasks/{taskId}/comments)
        Map<String, String> commentBody = Map.of("content", "Initial comment");
        
        Integer commentId = TestAuthHelper.authenticated(token)
            .contentType(ContentType.JSON)
            .body(commentBody)
            .post("/api/tasks/1/comments")
            .then()
            .statusCode(201)
            .extract().path("id");

        // 2. Редактировать комментарий (PUT /api/comments/{commentId})
        Map<String, String> updateBody = Map.of("content", "Updated comment");
        
        TestAuthHelper.authenticated(token)
            .contentType(ContentType.JSON)
            .body(updateBody)
            .put("/api/comments/" + commentId)
            .then()
            .statusCode(200)
            .body("content", is("Updated comment"))
            .body("isEdited", is(true));

        // 3. Удалить комментарий (DELETE /api/comments/{commentId})
        TestAuthHelper.authenticated(token)
            .delete("/api/comments/" + commentId)
            .then()
            .statusCode(204);
            
        // 4. Проверить отсутствие
        TestAuthHelper.authenticated(token)
            .get("/api/tasks/1/comments")
            .then()
            .statusCode(200)
            .body("size()", is(0));
    }
}
