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
public class ProjectResourceTest {

    @Test
    @Order(1)
    public void testGetAllProjectsEmpty() {
        given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(2)
    public void testCreateProject() {
        Project project = new Project();
        project.name = "Test Project";
        project.description = "Test Description";
        project.ownerUserId = 1;
        project.color = "#FF5733";

        given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test Project"))
            .body("description", equalTo("Test Description"))
            .body("color", equalTo("#FF5733"))
            .body("isActive", equalTo(true));
    }

    @Test
    @Order(3)
    public void testCreateProjectAndGet() {
        Project project = new Project();
        project.name = "Get Project";
        project.description = "Get Description";
        project.ownerUserId = 1;

        Integer projectId = given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when().get("/api/projects/" + projectId)
            .then()
            .statusCode(200)
            .body("id", equalTo(projectId))
            .body("name", equalTo("Get Project"))
            .body("description", equalTo("Get Description"));
    }

    @Test
    @Order(4)
    public void testUpdateProject() {
        Project project = new Project();
        project.name = "Update Project";
        project.description = "Original Description";
        project.ownerUserId = 1;

        Integer projectId = given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        Project updatedProject = new Project();
        updatedProject.name = "Updated Project";
        updatedProject.description = "Updated Description";
        updatedProject.color = "#33FF57";
        updatedProject.isActive = false;

        given()
            .contentType(ContentType.JSON)
            .body(updatedProject)
            .when().put("/api/projects/" + projectId)
            .then()
            .statusCode(200)
            .body("id", equalTo(projectId))
            .body("name", equalTo("Updated Project"))
            .body("description", equalTo("Updated Description"))
            .body("color", equalTo("#33FF57"))
            .body("isActive", equalTo(false));
    }

    @Test
    @Order(5)
    public void testGetProjectsByOwner() {
        Integer ownerId = 1;

        given()
            .when().get("/api/projects/owner/" + ownerId)
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(6)
    public void testGetActiveProjects() {
        given()
            .when().get("/api/projects/active")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(7)
    public void testGetProjectNotFound() {
        given()
            .when().get("/api/projects/99999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(8)
    public void testCreateProjectWithoutName() {
        Project project = new Project();
        project.name = "";
        project.ownerUserId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(9)
    public void testCreateProjectWithoutOwner() {
        Project project = new Project();
        project.name = "No Owner";
        project.ownerUserId = null;

        given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(10)
    public void testDeleteProject() {
        Project project = new Project();
        project.name = "Delete Project";
        project.description = "To be deleted";
        project.ownerUserId = 1;

        Integer projectId = given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when().delete("/api/projects/" + projectId)
            .then()
            .statusCode(204);

        given()
            .when().get("/api/projects/" + projectId)
            .then()
            .statusCode(404);
    }

    @Test
    @Order(11)
    public void testCreateProjectCreatesDefaultStatuses() {
        Project project = new Project();
        project.name = "Status Test Project";
        project.description = "Project to test default statuses";
        project.ownerUserId = 1;

        Integer projectId = given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        // Проверяем, что созданы 4 статуса по умолчанию
        given()
            .when().get("/api/project-statuses/project/" + projectId)
            .then()
            .statusCode(200)
            .body("$", hasSize(4))
            .body("statusName", hasItems("Todo", "In Progress", "Review", "Done"))
            .body("statusColor", hasItems("#E0E0E0", "#4A90D9", "#F5A623", "#7ED321"))
            .body("statusOrder", contains(0, 1, 2, 3))
            .body("find { it.statusName == 'Todo' }.isDefault", equalTo(true))
            .body("findAll { it.statusName != 'Todo' }.isDefault", everyItem(equalTo(false)));
    }

    @Test
    @Order(12)
    public void testCreateDefaultStatusesViaEndpoint() {
        // Создаём проект
        Project project = new Project();
        project.name = "Manual Status Project";
        project.description = "Project for manual status creation test";
        project.ownerUserId = 1;

        Integer projectId = given()
            .contentType(ContentType.JSON)
            .body(project)
            .when().post("/api/projects")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        // Вызываем endpoint создания дефолтных статусов (создаст ещё 4, итого 8)
        given()
            .when().post("/api/project-statuses/default/" + projectId)
            .then()
            .statusCode(201)
            .body("$", hasSize(8));
    }

    @Test
    @Order(13)
    public void testCreateDefaultStatusesNotFound() {
        given()
            .when().post("/api/project-statuses/default/99999")
            .then()
            .statusCode(404);
    }
}
