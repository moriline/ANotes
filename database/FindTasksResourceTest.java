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
public class FindTasksResourceTest {

    @Test
    @Order(1)
    public void testFindAllTasksEmpty() {
        Request.FindTasks find = new Request.FindTasks();

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(0));
    }

    @Test
    @Order(2)
    public void testCreateGlobalTaskForFind() {
        GlobalTask task = new GlobalTask();
        task.title = "Find Test Task";
        task.description = "Task for find testing";
        task.creatorUserId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(task)
            .when().post("/api/global-tasks")
            .then()
            .statusCode(201);
    }

    @Test
    @Order(3)
    public void testCreateProjectForFind() {
        Project project = new Project();
        project.name = "Find Project";
        project.description = "Project for find testing";
        project.ownerUserId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201);
    }

    @Test
    @Order(4)
    public void testCreateStatusForFind() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        ProjectStatus status = new ProjectStatus();
        status.projectId = projectId;
        status.statusName = "Todo";
        status.statusColor = "#E0E0E0";
        status.statusOrder = 0;

        given()
            .contentType(ContentType.JSON)
            .body(status)
            .when().post("/api/project-statuses")
            .then()
            .statusCode(201);
    }

    @Test
    @Order(5)
    public void testCreateLinkForFind() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Integer globalTaskId = given()
            .when().get("/api/global-tasks")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Integer statusId = given()
            .when().get("/api/project-statuses/project/" + projectId)
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        // Создаём пользователя для assignedUserId
        User user = new User();
        user.username = "findtestuser";
        user.email = "findtest@example.com";
        user.password = "password123";

        Integer userId = given()
            .contentType(ContentType.JSON)
            .body(user)
            .when().post("/api/users")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        ProjectTaskLink link = new ProjectTaskLink();
        link.projectId = projectId;
        link.globalTaskId = globalTaskId;
        link.statusId = statusId;
        link.assignedUserId = userId;

        given()
            .contentType(ContentType.JSON)
            .body(link)
            .when().post("/api/project-task-links")
            .then()
            .statusCode(201);
    }

    @Test
    @Order(6)
    public void testFindAllTasks() {
        Request.FindTasks find = new Request.FindTasks();

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].taskTitle", equalTo("Find Test Task"))
            .body("tasks[0].projectId", notNullValue())
            .body("tasks[0].globalTaskId", notNullValue());
    }

    @Test
    @Order(7)
    public void testFindByProjectId() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Request.FindTasks find = new Request.FindTasks();
        find.projectId = projectId;

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].projectId", equalTo(projectId));
    }

    @Test
    @Order(8)
    public void testFindByTitleSearch() {
        Request.FindTasks find = new Request.FindTasks();
        find.titleSearch = "Find Test";

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].taskTitle", containsString("Find Test"));
    }

    @Test
    @Order(9)
    public void testFindByAssignedUserId() {
        // Получаем ID созданного пользователя
        Integer userId = given()
            .when().get("/api/users")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Request.FindTasks find = new Request.FindTasks();
        find.assignedUserId = userId;

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].assignedUserId", equalTo(userId));
    }

    @Test
    @Order(10)
    public void testFindWithNoResults() {
        Request.FindTasks find = new Request.FindTasks();
        find.titleSearch = "Nonexistent Task Title XYZ";

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(0));
    }

    @Test
    @Order(11)
    public void testFindByProjectIdAndTitleSearch() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Request.FindTasks find = new Request.FindTasks();
        find.projectId = projectId;
        find.titleSearch = "Find";

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].projectId", equalTo(projectId))
            .body("tasks[0].taskTitle", containsString("Find"));
    }

    @Test
    @Order(12)
    public void testFindByIsArchived() {
        Request.FindTasks find = new Request.FindTasks();
        find.isArchived = false;

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].isArchived", equalTo(false));
    }

    @Test
    @Order(13)
    public void testFindByStatusId() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Integer statusId = given()
            .when().get("/api/project-statuses/project/" + projectId)
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Request.FindTasks find = new Request.FindTasks();
        find.statusId = statusId;

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(14)
    public void testFindByGlobalTaskId() {
        Integer globalTaskId = given()
            .when().get("/api/global-tasks")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        Request.FindTasks find = new Request.FindTasks();
        find.globalTaskId = globalTaskId;

        given()
            .contentType(ContentType.JSON)
            .body(find)
            .when().post("/api/find")
            .then()
            .statusCode(200)
            .body("tasks", hasSize(greaterThanOrEqualTo(1)))
            .body("tasks[0].globalTaskId", equalTo(globalTaskId));
    }
}
