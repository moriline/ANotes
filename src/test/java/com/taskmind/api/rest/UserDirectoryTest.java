package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Справочник пользователей {@code GET /api/users}: без него некого выбрать
 * исполнителем задачи и некого добавить в проект.
 */
@QuarkusTest
public class UserDirectoryTest {

    /** tester_olga из сида. */
    private static final int SEEDED_OLGA_ID = 4;

    @Inject TestDataCleanup cleanup;

    private String token;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        token = TestAuthHelper.registerAndLogin("dir-user-" + ts, "dir-user-" + ts + "@test.com", "Pass123!");
    }

    @Test
    public void findsUserByUsernameSubstring() {
        auth().get("/api/users?q=anna").then()
            .statusCode(200)
            .body("username", hasItem("dev_anna"))
            .body("username", not(hasItem("designer_max")));
    }

    @Test
    public void findsUserByDisplayName() {
        auth().get("/api/users?q=Max Designer").then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].username", equalTo("designer_max"))
            .body("[0].displayName", equalTo("Max Designer"));
    }

    @Test
    public void findsUserByEmailWithoutHandingTheEmailBack() {
        String body = auth().get("/api/users?q=olga@taskmanager.com").then()
            .statusCode(200)
            .body("username", hasItem("tester_olga"))
            .extract().body().asString();

        // В справочнике не должно быть ни почты, ни тем более хеша пароля.
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("email"), body);
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("password"), body);
    }

    @Test
    public void searchIgnoresCase() {
        auth().get("/api/users?q=ANNA").then()
            .statusCode(200)
            .body("username", hasItem("dev_anna"));
    }

    @Test
    public void emptyQueryReturnsTheDirectorySortedByUsername() {
        List<String> usernames = auth().get("/api/users").then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(5)))
            .extract().jsonPath().getList("username", String.class);

        assertEquals(usernames.stream().sorted().toList(), usernames, "справочник должен быть отсортирован");
    }

    @Test
    public void limitCapsTheResult() {
        auth().get("/api/users?limit=2").then()
            .statusCode(200)
            .body("$", hasSize(2));
    }

    @Test
    public void nonPositiveLimitIsRejectedAndHugeLimitIsClamped() {
        auth().get("/api/users?limit=0").then().statusCode(400);
        auth().get("/api/users?limit=-5").then().statusCode(400);
        auth().get("/api/users?limit=1000").then().statusCode(200);
    }

    @Test
    public void blockedUserDisappearsFromTheDirectory() {
        auth().get("/api/users?q=olga").then().body("username", hasItem("tester_olga"));

        auth().body("{\"isActive\": false}")
            .put("/api/admin/users/" + SEEDED_OLGA_ID + "/status").then().statusCode(200);

        auth().get("/api/users?q=olga").then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    public void unmatchedQueryReturnsNothing() {
        auth().get("/api/users?q=zzzzz-nobody").then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().get("/api/users").then().statusCode(401);
    }

    private RequestSpecification auth() {
        return TestAuthHelper.authenticated(token);
    }
}
