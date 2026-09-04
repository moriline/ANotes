package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.AdminUserStatusRequest;
import com.taskmind.api.dto.LoginRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * {@code /api/admin/users}: раньше у ресурса не было ни {@code @RolesAllowed}, ни
 * проверки прав, а GET отдавал сущности вместе с bcrypt-хешами паролей.
 *
 * <p>Админом здесь становится не «любой залогиненный», а обладатель флага
 * {@code users.isAdmin} — из сида это пользователь {@code admin}.
 */
@QuarkusTest
public class AdminUserResourceTest {

    /** Пользователь admin из seed-h2-v4.sql: userId = 1, isAdmin = TRUE. */
    private static final int ADMIN_ID = 1;
    private static final int OLGA_ID = 4;

    @Inject TestDataCleanup cleanup;

    private String adminToken;
    private String userToken;
    private Integer userId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        adminToken = loginAsSeededAdmin();
        long ts = System.nanoTime();
        userToken = TestAuthHelper.registerAndLogin("admin-outsider-" + ts, "admin-outsider-" + ts + "@test.com", "Pass123!");
        userId = auth(userToken).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
    }

    @Test
    public void anonymousIsRejected() {
        given().get("/api/admin/users").then().statusCode(401);
        given().delete("/api/admin/users/" + OLGA_ID).then().statusCode(401);
        given().contentType(ContentType.JSON).body(new AdminUserStatusRequest(false))
            .put("/api/admin/users/" + OLGA_ID + "/status").then().statusCode(401);
    }

    /** Обычный пользователь — именно 403: он аутентифицирован, но не администратор. */
    @Test
    public void ordinaryUserIsForbiddenEverywhere() {
        auth(userToken).get("/api/admin/users").then().statusCode(403);
        auth(userToken).delete("/api/admin/users/" + OLGA_ID).then().statusCode(403);
        auth(userToken).body(new AdminUserStatusRequest(false))
            .put("/api/admin/users/" + OLGA_ID + "/status").then().statusCode(403);
    }

    /** Главное в этом тесте: в ответе нет поля password ни у кого. */
    @Test
    public void adminListNeverExposesPasswordHashes() {
        auth(adminToken).get("/api/admin/users").then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(5)))
            .body("password", everyItem(nullValue()))
            .body("find { it.id == " + ADMIN_ID + " }.username", equalTo("admin"))
            .body("find { it.id == " + ADMIN_ID + " }.isAdmin", equalTo(true))
            .body("find { it.id == " + OLGA_ID + " }.isAdmin", equalTo(false));
    }

    @Test
    public void adminCanBlockAndUnblockUser() {
        auth(adminToken).body(new AdminUserStatusRequest(false))
            .put("/api/admin/users/" + OLGA_ID + "/status").then()
            .statusCode(200)
            .body("isActive", equalTo(false))
            .body("password", nullValue());

        auth(adminToken).body(new AdminUserStatusRequest(true))
            .put("/api/admin/users/" + OLGA_ID + "/status").then()
            .statusCode(200)
            .body("isActive", equalTo(true));
    }

    @Test
    public void adminCanDeleteUser() {
        auth(adminToken).delete("/api/admin/users/" + userId).then().statusCode(204);
        auth(adminToken).get("/api/admin/users").then()
            .statusCode(200)
            .body("id", not(hasItem(userId)));
    }

    @Test
    public void adminCannotDeleteOrBlockHimself() {
        auth(adminToken).delete("/api/admin/users/" + ADMIN_ID).then().statusCode(400);
        auth(adminToken).body(new AdminUserStatusRequest(false))
            .put("/api/admin/users/" + ADMIN_ID + "/status").then().statusCode(400);
    }

    @Test
    public void missingUserIsNotFoundAndEmptyBodyIsRejected() {
        auth(adminToken).delete("/api/admin/users/99999").then().statusCode(404);
        auth(adminToken).body(new AdminUserStatusRequest(false))
            .put("/api/admin/users/99999/status").then().statusCode(404);
        auth(adminToken).body(new AdminUserStatusRequest(null))
            .put("/api/admin/users/" + OLGA_ID + "/status").then().statusCode(400);
    }

    private static String loginAsSeededAdmin() {
        return given().contentType(ContentType.JSON)
            .body(new LoginRequest("admin", "admin123"))
            .post("/api/auth/login").then().statusCode(200)
            .extract().jsonPath().getString("token");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
