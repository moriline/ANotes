package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Verifies that POST /api/find only ever returns tasks from projects the caller
 * owns or is a member of.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FindResourceAccessTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;
    @Inject MembershipRepository membershipRepository;

    private static String tokenA;
    private static String tokenB;
    private static Integer userBId;
    private static Integer projectAId;
    private static Integer projectBId;
    private static boolean initialized = false;

    @BeforeEach
    void setUp() {
        if (!initialized) {
            cleanup.clearAll();
            long ts = System.currentTimeMillis();
            tokenA = TestAuthHelper.registerAndLogin("access-a-" + ts, "access-a-" + ts + "@test.com", "Pass123!");
            tokenB = TestAuthHelper.registerAndLogin("access-b-" + ts, "access-b-" + ts + "@test.com", "Pass123!");
            userBId = auth(tokenB).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
            initialized = true;
        }
    }

    @Test
    @Order(1)
    void prepareProjectsAndTasks() {
        projectAId = createProject(tokenA, "access-project-a");
        projectBId = createProject(tokenB, "access-project-b");
        createTask(tokenA, projectAId, "ClassifiedAlpha");
        createTask(tokenB, projectBId, "ClassifiedBravo");
    }

    @Test
    @Order(2)
    void ownerSeesOnlyOwnTasks() {
        auth(tokenA).body("{}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks.title", hasItem("ClassifiedAlpha"))
            .body("tasks.title", not(hasItem("ClassifiedBravo")));
    }

    @Test
    @Order(3)
    void strangerDoesNotSeeForeignTasksWithEmptyBody() {
        auth(tokenB).body("{}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks.title", hasItem("ClassifiedBravo"))
            .body("tasks.title", not(hasItem("ClassifiedAlpha")));
    }

    @Test
    @Order(4)
    void titleSearchIsScopedToAccessibleProjects() {
        auth(tokenB).body("{\"titleSearch\": \"Classified\"}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks.title", hasItem("ClassifiedBravo"))
            .body("tasks.title", not(hasItem("ClassifiedAlpha")));
    }

    @Test
    @Order(5)
    void targetingForeignProjectByIdReturnsEmpty() {
        auth(tokenB).body("{\"projectId\": " + projectAId + "}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks", hasSize(0));
    }

    @Test
    @Order(6)
    void memberSeesProjectTasksAfterBeingAdded() {
        membershipRepository.save(ProjectMembership.create(projectAId, userBId, ROLE_DEVELOPER));

        auth(tokenB).body("{}")
        .when().post("/api/find")
        .then()
            .statusCode(200)
            .body("tasks.title", hasItem("ClassifiedAlpha"))
            .body("tasks.title", hasItem("ClassifiedBravo"));
    }

    @Test
    @Order(7)
    void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/find")
        .then()
            .statusCode(401);
    }

    private static Integer createProject(String token, String name) {
        Response r = auth(token).body("{\"name\": \"" + name + "\"}")
            .when().post("/api/projects")
            .then().statusCode(201).extract().response();
        return r.jsonPath().getInt("id");
    }

    private static void createTask(String token, Integer projectId, String title) {
        auth(token).body("{\"title\": \"" + title + "\"}")
            .when().post("/api/projects/" + projectId + "/tasks")
            .then().statusCode(201);
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
