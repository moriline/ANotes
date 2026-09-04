package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.ProjectRequest;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectResourceTest {

    @Inject TestDataCleanup cleanup;

    private static final String TEST_PROJECT_NAME = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private static Integer createdProjectId;
    private static String token;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            token = TestAuthHelper.registerAndLogin("projuser-" + System.currentTimeMillis(), "proj@test.com", "Pass123!");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void shouldCreateProject() {
        Response response = authenticated()
            .body(new ProjectRequest(TEST_PROJECT_NAME, "Test Project Description"))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(201)
            .body("name", equalTo(TEST_PROJECT_NAME))
            .body("description", equalTo("Test Project Description"))
            .body("isActive", equalTo(true))
            .body("id", notNullValue())
            .body("ownerUserId", notNullValue())
        .extract().response();

        createdProjectId = response.jsonPath().getInt("id");
    }

    @Test
    @Order(2)
    void shouldListActiveProjects() {
        authenticated()
        .when()
            .get("/api/projects")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("name", hasItem(TEST_PROJECT_NAME));
    }

    @Test
    @Order(3)
    void shouldRejectDuplicateName() {
        authenticated()
            .body(new ProjectRequest(TEST_PROJECT_NAME, null))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(409);
    }

    @Test
    @Order(4)
    void shouldDeleteProject() {
        authenticated()
        .when()
            .delete("/api/projects/" + createdProjectId)
        .then()
            .statusCode(204);

        authenticated()
        .when().get("/api/projects")
        .then()
            .body("name", not(hasItem(TEST_PROJECT_NAME)));
    }

    @Test
    @Order(5)
    void shouldRejectUnauthenticatedAccess() {
        given()
            .contentType(io.restassured.http.ContentType.JSON)
            .body(new ProjectRequest("unauth-project", null))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(401);
    }

    @Test
    @Order(6)
    void createStoresColorIconAndTagsWhenGiven() {
        authenticated()
            .body(new ProjectRequest("Color Project", "Colored", "#9B59B6", "palette", List.of("ui", "brand")))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(201)
            .body("name", equalTo("Color Project"))
            .body("color", equalTo("#9B59B6"))
            .body("icon", equalTo("palette"))
            .body("tags", contains("ui", "brand"));
    }

    @Test
    @Order(7)
    void createWithoutAppearanceFallsBackToDefaultColorAndEmptyTags() {
        authenticated()
            .body(new ProjectRequest("Plain Project", null))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(201)
            .body("color", equalTo("#4A90D9"))
            .body("icon", nullValue())
            .body("tags", empty());
    }

    @Test
    @Order(8)
    void createRejectsAMalformedColor() {
        authenticated()
            .body(new ProjectRequest("Bad Color Project", null, "purple", null, null))
        .when()
            .post("/api/projects")
        .then()
            .statusCode(400);
    }

    private RequestSpecification authenticated() {
        return TestAuthHelper.authenticated(token);
    }
}
