package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.PermissionCheckRequest;
import com.taskmind.domain.model.Action;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class RoleIntegrationTest {

    // В seed-h2-v4.sql:
    // admin (id 1) - Admin (can delete project)
    // dev_anna (id 2) - Developer (can create task, but NOT delete project)
    // tester_olga (id 4) - Guest (only read)

    @Inject TestDataCleanup cleanup;

    /**
     * Роли берутся из сида, а AdminUserResourceTest умеет удалять оттуда
     * пользователя 2 — без восстановления сида результат зависел бы от порядка классов.
     */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void testAdminCanDeleteProject() {
        String token = TestAuthHelper.registerAndLogin("admin_user_1", "admin1@test.com", "password");
        checkPermission(token, 1, 1, Action.PROJECT_DELETE, true);
    }

    @Test
    public void testDeveloperCannotDeleteProject() {
        String token = TestAuthHelper.registerAndLogin("dev_user_1", "dev1@test.com", "password");
        checkPermission(token, 2, 1, Action.PROJECT_DELETE, false);
    }

    @Test
    public void testGuestCannotCreateTask() {
        String token = TestAuthHelper.registerAndLogin("guest_user_1", "guest1@test.com", "password");
        checkPermission(token, 4, 1, Action.TASK_CREATE, false);
    }

    @Test
    public void testDeveloperCanCreateTask() {
        String token = TestAuthHelper.registerAndLogin("dev_user_2", "dev2@test.com", "password");
        checkPermission(token, 2, 1, Action.TASK_CREATE, true);
    }

    private void checkPermission(String token, Integer userId, Integer projectId, Action action, boolean expected) {
        TestAuthHelper.authenticated(token)
            .body(new PermissionCheckRequest(userId, projectId, action.getValue()))
            .post("/api/permissions/check")
            .then()
            .statusCode(200)
            .body("allowed", is(expected));
    }
}
