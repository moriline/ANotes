package com.taskmind;

import com.taskmind.api.rest.TestAuthHelper;
import com.taskmind.infrastructure.db.CommentEntity;
import com.taskmind.infrastructure.db.ProjectEntity;
import com.taskmind.infrastructure.db.ProjectRoleEntity;
import com.taskmind.infrastructure.db.TaskEntity;
import com.taskmind.infrastructure.db.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяет саму тестовую инфраструктуру: {@link TestDataCleanup} обязан
 * приводить базу к одному и тому же состоянию, иначе результат остальных тестов
 * снова начнёт зависеть от порядка запуска классов.
 */
@QuarkusTest
public class TestDataCleanupTest {

    @Inject TestDataCleanup cleanup;

    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void shouldRestoreSeedDataWithStableIds() {
        assertEquals(4, UserEntity.count(), "сид должен вернуть ровно 4 пользователей");
        assertEquals(3, ProjectEntity.count(), "сид должен вернуть ровно 3 проекта");
        assertEquals(4, TaskEntity.count(), "сид должен вернуть ровно 4 задачи");
        assertEquals(4, CommentEntity.count(), "сид должен вернуть ровно 4 комментария");

        UserEntity admin = UserEntity.findById(1);
        assertNotNull(admin, "userId=1 должен существовать после сброса");
        assertEquals("admin", admin.username);

        ProjectEntity firstProject = ProjectEntity.findById(1);
        assertNotNull(firstProject, "projectId=1 должен существовать после сброса");
        assertEquals("Web Site Redesign", firstProject.name);

        assertNotNull(TaskEntity.findById(1), "taskId=1 должен существовать после сброса");
    }

    @Test
    public void shouldKeepReferenceRolesUntouched() {
        assertEquals(5, ProjectRoleEntity.count(), "справочник ролей чистить нельзя");
        ProjectRoleEntity admin = ProjectRoleEntity.findById(1);
        assertNotNull(admin);
        assertEquals("Admin", admin.roleName);
    }

    @Test
    public void shouldRestartIdentityCountersSoNewRowsAreDeterministic() {
        String token = TestAuthHelper.registerAndLogin("cleanup_probe", "cleanup_probe@test.com", "Pass123!");

        // Сид занимает userId 1..4 и projectId 1..3, значит следующие записи
        // обязаны получить 5 и 4 — иначе счётчики identity не перезапускаются
        // и тесты, завязанные на конкретные id, снова поплывут.
        Integer newUserId = TestAuthHelper.authenticated(token)
            .get("/api/users/me")
            .then().statusCode(200)
            .extract().jsonPath().getInt("id");
        assertEquals(5, newUserId);

        Integer newProjectId = TestAuthHelper.authenticated(token)
            .body("{\"name\": \"cleanup-probe-project\"}")
            .post("/api/projects")
            .then().statusCode(201)
            .extract().jsonPath().getInt("id");
        assertEquals(4, newProjectId);
    }

    @Test
    public void shouldBeIdempotentWhenCalledTwice() {
        cleanup.clearAll();
        cleanup.clearAll();

        assertEquals(4, UserEntity.count());
        assertEquals(3, ProjectEntity.count());
        assertEquals(4, TaskEntity.count());

        UserEntity admin = UserEntity.findById(1);
        assertNotNull(admin);
        assertEquals("admin", admin.username);
    }
}
