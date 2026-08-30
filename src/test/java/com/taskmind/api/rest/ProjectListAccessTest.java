package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * GET /api/projects обязан показывать только те проекты, которые пользователь
 * действительно видит: свои и те, где он участник.
 */
@QuarkusTest
public class ProjectListAccessTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;
    @Inject MembershipRepository membershipRepository;

    private String tokenA;
    private String tokenB;
    private Integer userBId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        tokenA = TestAuthHelper.registerAndLogin("plist-a-" + ts, "plist-a-" + ts + "@test.com", "Pass123!");
        tokenB = TestAuthHelper.registerAndLogin("plist-b-" + ts, "plist-b-" + ts + "@test.com", "Pass123!");
        userBId = auth(tokenB).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void ownerSeesOwnProject() {
        createProject(tokenA, "plist-own");

        auth(tokenA).get("/api/projects").then()
            .statusCode(200)
            .body("name", hasItem("plist-own"));
    }

    @Test
    public void strangerDoesNotSeeForeignProject() {
        createProject(tokenA, "plist-secret");
        createProject(tokenB, "plist-mine");

        auth(tokenB).get("/api/projects").then()
            .statusCode(200)
            .body("name", hasItem("plist-mine"))
            .body("name", not(hasItem("plist-secret")));
    }

    @Test
    public void freshUserSeesNoSeededProjects() {
        // Проекты из сида принадлежат сидовым пользователям, новичок их видеть не должен.
        auth(tokenA).get("/api/projects").then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    public void memberSeesProjectAfterBeingAdded() {
        Integer projectId = createProject(tokenA, "plist-shared");

        auth(tokenB).get("/api/projects").then()
            .statusCode(200)
            .body("name", not(hasItem("plist-shared")));

        membershipRepository.save(ProjectMembership.create(projectId, userBId, ROLE_DEVELOPER));

        auth(tokenB).get("/api/projects").then()
            .statusCode(200)
            .body("name", hasItem("plist-shared"));
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON)
            .get("/api/projects")
            .then().statusCode(401);
    }

    private Integer createProject(String token, String name) {
        return auth(token)
            .body("{\"name\": \"" + name + "\"}")
            .post("/api/projects")
            .then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
