package org.taskone;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GlobalTaskResourceTest {

    @Test
    @Order(1)
    public void testGetAllTasksEmpty() {
        given()
            .when().get("/api/global-tasks")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(2)
    public void testCreateTask() {
        GlobalTask task = new GlobalTask();
        task.title = "Test Task";
        task.description = "Test Description";
        task.creatorUserId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo("Test Task"))
            .body("description", equalTo("Test Description"));
    }

    @Test
    @Order(3)
    public void testCreateTaskAndGet() {
        GlobalTask task = new GlobalTask();
        task.title = "Get Task";
        task.description = "Get Description";
        task.creatorUserId = 1;

        Integer taskId = given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when().get("/api/global-tasks/" + taskId)
            .then()
            .statusCode(200)
            .body("id", equalTo(taskId))
            .body("title", equalTo("Get Task"))
            .body("description", equalTo("Get Description"));
    }

    @Test
    @Order(4)
    public void testUpdateTask() {
        GlobalTask task = new GlobalTask();
        task.title = "Update Task";
        task.description = "Original Description";
        task.creatorUserId = 1;

        Integer taskId = given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        GlobalTask updatedTask = new GlobalTask();
        updatedTask.title = "Updated Task";
        updatedTask.description = "Updated Description";
        updatedTask.dueDate = System.currentTimeMillis() + 86400000L;

        given()
            .contentType(ContentType.JSON)
            .body(updatedTask)
            .when().put("/api/global-tasks/" + taskId)
            .then()
            .statusCode(200)
            .body("id", equalTo(taskId))
            .body("title", equalTo("Updated Task"))
            .body("description", equalTo("Updated Description"));
    }

    @Test
    @Order(5)
    public void testGetTasksByCreator() {
        Integer creatorId = 1;

        given()
            .when().get("/api/global-tasks/creator/" + creatorId)
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(6)
    public void testSearchTasks() {
        given()
            .when().get("/api/global-tasks/search?title=Updated")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(8)
    public void testGetTaskNotFound() {
        given()
            .when().get("/api/global-tasks/99999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(9)
    public void testCreateTaskWithoutTitle() {
        GlobalTask task = new GlobalTask();
        task.title = "";
        task.creatorUserId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(10)
    public void testCreateTaskWithoutCreator() {
        GlobalTask task = new GlobalTask();
        task.title = "No Creator";
        task.creatorUserId = null;

        given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(11)
    public void testDeleteTask() {
        GlobalTask task = new GlobalTask();
        task.title = "Delete Task";
        task.description = "To be deleted";
        task.creatorUserId = 1;

        Integer taskId = given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when().delete("/api/global-tasks/" + taskId)
            .then()
            .statusCode(204);

        given()
            .when().get("/api/global-tasks/" + taskId)
            .then()
            .statusCode(404);
    }
}
