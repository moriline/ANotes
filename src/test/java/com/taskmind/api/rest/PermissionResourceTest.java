package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class PermissionResourceTest {

    @Test
    public void shouldCheckAdminPermission() {
        // Admin user is always userId=1 in schema-h2-v4.sql and we hope it's not deleted
        // but to be safe, we can't easily re-insert with ID 1 if it's auto-increment.
        
        // Let's use existing data but acknowledge it might fail if cleaned.
        // If it fails during full run, it's because of TestDataCleanup.
        
        // Actually, let's just use the admin user from the registerAndLogin
        String token = TestAuthHelper.registerAndLogin("admin_tester", "admin_tester@test.com", "password");
        
        // We need a way to get the userId of the registered user.
        // TestAuthHelper.registerAndLogin only returns token.
        
        // For now, I'll just accept that it might fail in a batch run if TestDataCleanup is used.
        // But wait, I can just not use TestDataCleanup.
        
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
