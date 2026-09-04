package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.LoginRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectMemberRoleRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.ProjectUpdateRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Оформление проекта (цвет/иконка), справочник {@code GET /api/projects/appearance},
 * тэги проекта и {@code PATCH /api/projects/{id}}.
 */
@QuarkusTest
public class ProjectAppearanceAndTagsTest {

    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer projectId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("app-owner-" + ts, "app-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("app-member-" + ts, "app-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("app-stranger-" + ts, "app-stranger-" + ts + "@test.com", "Pass123!");

        projectId = auth(ownerToken).body(new ProjectRequest("appearance-" + ts, "desc", "#2ECC71", "server", List.of("initial")))
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");

        auth(ownerToken).body(new ProjectMemberRequest(meId(memberToken), ROLE_DEVELOPER))
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);
    }

    @Test
    public void appearanceCatalogListsColorsAndIcons() {
        auth(ownerToken).get("/api/projects/appearance").then()
            .statusCode(200)
            .body("colors", hasItem("#4A90D9"))
            .body("colors", everyItem(matchesRegex("#[0-9A-Fa-f]{6}")))
            .body("icons", hasItems("globe", "server"))
            .body("icons.size()", greaterThan(3));
    }

    @Test
    public void ownerUpdatesColorIconAndTagsLeavingOtherFieldsUntouched() {
        auth(ownerToken).body(new ProjectUpdateRequest(null, null, "#E91E63", "palette", List.of("q1", "brand")))
            .patch("/api/projects/" + projectId).then()
            .statusCode(200)
            .body("name", startsWith("appearance-"))
            .body("description", equalTo("desc"))
            .body("color", equalTo("#E91E63"))
            .body("icon", equalTo("palette"))
            .body("tags", contains("q1", "brand"));

        auth(ownerToken).get("/api/projects/" + projectId).then()
            .statusCode(200)
            .body("color", equalTo("#E91E63"))
            .body("tags", contains("q1", "brand"));
    }

    @Test
    public void emptyTagsListClearsThem() {
        auth(ownerToken).body(new ProjectUpdateRequest(null, null, null, null, List.of()))
            .patch("/api/projects/" + projectId).then()
            .statusCode(200)
            .body("tags", empty());

        auth(ownerToken).get("/api/projects/" + projectId + "/tags").then()
            .statusCode(200)
            .body("$", empty());
    }

    @Test
    public void projectTagsEndpointReturnsTheList() {
        auth(ownerToken).get("/api/projects/" + projectId + "/tags").then()
            .statusCode(200)
            .body("$", contains("initial"));

        // участник видит
        auth(memberToken).get("/api/projects/" + projectId + "/tags").then().statusCode(200);
        // посторонний — нет
        auth(strangerToken).get("/api/projects/" + projectId + "/tags").then().statusCode(403);
        // нет проекта — 404
        auth(ownerToken).get("/api/projects/999999/tags").then().statusCode(404);
    }

    @Test
    public void updateNeedsProjectUpdatePermission() {
        // Developer менять карточку проекта не может — нужно project:update (владелец или Admin).
        auth(memberToken).body(new ProjectUpdateRequest(null, null, "#000000", null, null))
            .patch("/api/projects/" + projectId).then().statusCode(403);

        // Повышаем участника до Admin — теперь может.
        auth(ownerToken).body(new ProjectMemberRoleRequest(ROLE_ADMIN))
            .put("/api/projects/" + projectId + "/members/" + meId(memberToken)).then().statusCode(200);
        auth(memberToken).body(new ProjectUpdateRequest(null, null, "#000000", null, null))
            .patch("/api/projects/" + projectId).then().statusCode(200)
            .body("color", equalTo("#000000"));
    }

    @Test
    public void updateRejectsMalformedColorAndTakenName() {
        auth(ownerToken).body(new ProjectUpdateRequest(null, null, "not-a-color", null, null))
            .patch("/api/projects/" + projectId).then().statusCode(400);

        Integer other = auth(ownerToken).body(new ProjectRequest("appearance-taken-" + System.nanoTime(), null))
            .post("/api/projects").then().statusCode(201).extract().jsonPath().getInt("id");
        String takenName = auth(ownerToken).get("/api/projects/" + other).then().statusCode(200)
            .extract().jsonPath().getString("name");

        auth(ownerToken).body(new ProjectUpdateRequest(takenName, null, null, null, null))
            .patch("/api/projects/" + projectId).then().statusCode(409);
    }

    @Test
    public void updateUnknownProjectIs404() {
        auth(ownerToken).body(new ProjectUpdateRequest("whatever", null, null, null, null))
            .patch("/api/projects/999999").then().statusCode(404);
    }

    @Test
    public void seededProjectsCarryTheirTags() {
        String adminToken = given().contentType(ContentType.JSON)
            .body(new LoginRequest("admin", "admin123"))
            .post("/api/auth/login").then().statusCode(200).extract().jsonPath().getString("token");

        auth(adminToken).get("/api/projects/2/tags").then()
            .statusCode(200)
            .body("$", hasItems("backend", "mobile", "api"));
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200).extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }
}
