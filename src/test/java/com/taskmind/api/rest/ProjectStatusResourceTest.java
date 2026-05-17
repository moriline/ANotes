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
class ProjectStatusResourceTest {

    @Inject TestDataCleanup cleanup;

    private static Integer projectId;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("statususer-" + System.currentTimeMillis(), "status@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void shouldCreateProjectForStatus() {
        Response response = authenticated()
            .body("{\"name\": \"status-test-project\"}")
        .when().post("/api/projects")
        .then()
            .statusCode(201)
        .extract().response();

        projectId = response.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldCreateStatus() {
        authenticated()
            .body("{\"projectId\": " + projectId + ", \"statusName\": \"Todo\", \"statusColor\": \"#E0E0E0\", \"statusOrder\": 0, \"isDefault\": true}")
        .when()
            .post("/api/project-statuses")
        .then()
            .statusCode(201)
            .body("statusName", equalTo("Todo"))
            .body("projectId", equalTo(projectId))
            .body("isDefault", equalTo(true));
    }

    @Test
    @Order(3)
    void shouldListStatusesByProject() {
        authenticated()
        .when()
            .get("/api/project-statuses/project/" + projectId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("statusName", hasItem("Todo"));
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
