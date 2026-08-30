package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AdminUserResourceTest {

    @Inject TestDataCleanup cleanup;

    /**
     * Тест блокирует пользователя 4 и удаляет пользователя 2 из сида. Без сброса
     * это ломало RoleIntegrationTest, если тот запускался следом.
     */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void testAdminManagement() {
        String token = TestAuthHelper.registerAndLogin("admin_mgmt_tester", "admin_mgmt@test.com", "password");

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
