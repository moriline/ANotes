package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AdminUserResourceTest {

    @Test
    public void testAdminManagement() {
        // Use registerAndLogin for admin user
        String token = TestAuthHelper.registerAndLogin("admin_tester", "admin_tester@test.com", "password");

        // 1. Получение списка пользователей
        TestAuthHelper.authenticated(token)
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)));

        // 2. Блокировка пользователя (например, olga с id 4)
        TestAuthHelper.authenticated(token)
            .contentType("application/json")
            .body("{\"isActive\": false}")
            .put("/api/admin/users/4/status")
            .then()
            .statusCode(200);

        // 3. Удаление пользователя (например, dev_anna с id 2)
        TestAuthHelper.authenticated(token)
            .delete("/api/admin/users/2")
            .then()
            .statusCode(204);
    }
}
