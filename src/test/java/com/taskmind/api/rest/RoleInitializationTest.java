package com.taskmind.api.rest;

import com.taskmind.infrastructure.db.ProjectRoleEntity;
import com.taskmind.infrastructure.db.UserEntity;
import com.taskmind.infrastructure.db.ProjectEntity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RoleInitializationTest {

    @Test
    public void testRolesInitialization() {
        long count = ProjectRoleEntity.count();
        assertTrue(count >= 5, "Expected at least 5 roles, found: " + count);
        
        ProjectRoleEntity admin = ProjectRoleEntity.find("roleName", "Admin").firstResult();
        assertNotNull(admin);
        assertTrue(admin.permissions.contains(com.taskmind.domain.model.Action.ROLE_MANAGE));
    }

    @Test
    public void testUsersInitialization() {
        long count = UserEntity.count();
        assertTrue(count >= 4, "Expected at least 4 users, found: " + count);
    }

    @Test
    public void testProjectsInitialization() {
        long count = ProjectEntity.count();
        assertTrue(count >= 3, "Expected at least 3 projects, found: " + count);
    }
}
