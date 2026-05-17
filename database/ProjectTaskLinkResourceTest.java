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
public class ProjectTaskLinkResourceTest {

    @Test
    @Order(1)
    public void testGetAllLinksEmpty() {
        given()
            .when().get("/api/project-task-links")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(2)
    public void testCreateGlobalTaskFirst() {
        GlobalTask task = new GlobalTask();
        task.title = "Linked Task";
        task.description = "Task to be linked to project";
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
    public void testCreateProjectFirst() {
        Project project = new Project();
        project.name = "Link Project";
        project.description = "Project for linking tasks";
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
    public void testCreateLink() {
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

        ProjectTaskLink link = new ProjectTaskLink();
        link.projectId = projectId;
        link.globalTaskId = globalTaskId;

        given()
            .contentType(ContentType.JSON)
            .body(link)
            .when().post("/api/project-task-links")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("projectId", equalTo(projectId))
            .body("globalTaskId", equalTo(globalTaskId));
    }

    @Test
    @Order(5)
    public void testGetLinksByProject() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        given()
            .when().get("/api/project-task-links/project/" + projectId)
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(6)
    public void testGetActiveLinksByProject() {
        Integer projectId = given()
            .when().get("/api/projects")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        given()
            .when().get("/api/project-task-links/project/" + projectId + "/active")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(7)
    public void testUpdateLink() {
        Integer linkId = given()
            .when().get("/api/project-task-links")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        ProjectTaskLink updatedLink = new ProjectTaskLink();
        updatedLink.tags = "test-tag";
        updatedLink.isArchived = false;

        given()
            .contentType(ContentType.JSON)
            .body(updatedLink)
            .when().put("/api/project-task-links/" + linkId)
            .then()
            .statusCode(200)
            .body("id", equalTo(linkId))
            .body("tags", equalTo("test-tag"));
    }

    @Test
    @Order(8)
    public void testArchiveLink() {
        Integer linkId = given()
            .when().get("/api/project-task-links")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        given()
            .when().patch("/api/project-task-links/" + linkId + "/archive")
            .then()
            .statusCode(200)
            .body("id", equalTo(linkId))
            .body("isArchived", equalTo(true));
    }

    @Test
    @Order(9)
    public void testGetLinkNotFound() {
        given()
            .when().get("/api/project-task-links/99999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(10)
    public void testCreateLinkWithoutProjectId() {
        ProjectTaskLink link = new ProjectTaskLink();
        link.projectId = null;
        link.globalTaskId = 1;

        given()
            .contentType(ContentType.JSON)
            .body(link)
            .when().post("/api/project-task-links")
            .then()
            .statusCode(400);
    }

    @Test
    @Order(11)
    public void testDeleteLink() {
        Integer linkId = given()
            .when().get("/api/project-task-links")
            .then()
            .statusCode(200)
            .extract()
            .path("id[-1]");

        given()
            .when().delete("/api/project-task-links/" + linkId)
            .then()
            .statusCode(204);

        given()
            .when().get("/api/project-task-links/" + linkId)
            .then()
            .statusCode(404);
    }
}
