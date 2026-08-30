package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class PermissionResourceTest {

    @Inject TestDataCleanup cleanup;

    /**
     * Тест опирается на сид: userId=1 (admin) состоит в projectId=1 с ролью Admin.
     * Восстанавливаем сид перед каждым методом, иначе результат зависит от того,
     * какой класс отработал раньше в общем прогоне.
     */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void shouldCheckAdminPermission() {
        String token = TestAuthHelper.registerAndLogin(
            "permission_tester", "permission_tester@test.com", "password");

        Map<String, Object> checkBody = Map.of(
            "userId", 1,
            "projectId", 1,
            "action", "task:create"
        );

        TestAuthHelper.authenticated(token)
            .body(checkBody)
            .post("/api/permissions/check")
            .then()
            .statusCode(200)
            .body("allowed", is(true));
    }
}
