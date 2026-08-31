package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Управление составом проекта: {@code /api/projects/{projectId}/members}.
 * Раньше выдать человеку роль в проекте можно было только вставкой в базу.
 */
@QuarkusTest
public class ProjectMemberResourceTest {

    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_MANAGER = 2;
    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private Integer ownerId;
    private Integer memberId;
    private Integer strangerId;
    private Integer projectId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("mem-owner-" + ts, "mem-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("mem-user-" + ts, "mem-user-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("mem-stranger-" + ts, "mem-stranger-" + ts + "@test.com", "Pass123!");

        ownerId = meId(ownerToken);
        memberId = meId(memberToken);
        strangerId = meId(strangerToken);

        projectId = auth(ownerToken).body("{\"name\": \"members-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void creatorBecomesAdminMemberOfHisOwnProject() {
        auth(ownerToken).get(membersPath()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].userId", equalTo(ownerId))
            .body("[0].roleId", equalTo(ROLE_ADMIN))
            .body("[0].roleName", equalTo("Admin"));
    }

    @Test
    public void ownerCanAddMemberAndSeeHimInTheList() {
        addMember(ownerToken, memberId, ROLE_DEVELOPER)
            .statusCode(201)
            .body("userId", equalTo(memberId))
            .body("roleId", equalTo(ROLE_DEVELOPER))
            .body("roleName", equalTo("Developer"))
            .body("username", notNullValue())
            .body("joinedAt", notNullValue());

        auth(ownerToken).get(membersPath()).then()
            .statusCode(200)
            .body("$", hasSize(2))
            .body("userId", hasItems(ownerId, memberId));
    }

    @Test
    public void addedMemberGetsAccessToTheProjectAndItsTasks() {
        Integer taskId = auth(ownerToken).body("{\"title\": \"Общая задача\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(memberToken).get("/api/projects").then()
            .statusCode(200)
            .body("name", not(hasItem("members-project")));

        addMember(ownerToken, memberId, ROLE_DEVELOPER).statusCode(201);

        auth(memberToken).get("/api/projects").then()
            .statusCode(200)
            .body("name", hasItem("members-project"));

        auth(memberToken).body("{}").post("/api/find").then()
            .statusCode(200)
            .body("tasks.id", hasItem(taskId));
    }

    @Test
    public void addingTheSameUserTwiceIsRejected() {
        addMember(ownerToken, memberId, ROLE_DEVELOPER).statusCode(201);
        addMember(ownerToken, memberId, ROLE_MANAGER).statusCode(409);
    }

    @Test
    public void addingUnknownUserOrRoleIsRejected() {
        addMember(ownerToken, 99999, ROLE_DEVELOPER).statusCode(400);
        addMember(ownerToken, memberId, 999).statusCode(400);
    }

    @Test
    public void strangerCanNeitherListNorAddMembers() {
        auth(strangerToken).get(membersPath()).then().statusCode(403);
        addMember(strangerToken, strangerId, ROLE_DEVELOPER).statusCode(403);
    }

    @Test
    public void plainMemberCannotManageMembersButAdminMemberCan() {
        addMember(ownerToken, memberId, ROLE_DEVELOPER).statusCode(201);

        // Developer видит список, но менять состав не может.
        auth(memberToken).get(membersPath()).then().statusCode(200);
        addMember(memberToken, strangerId, ROLE_DEVELOPER).statusCode(403);

        // После повышения до Admin — может.
        auth(ownerToken).body("{\"roleId\": " + ROLE_ADMIN + "}")
            .put(membersPath() + "/" + memberId).then().statusCode(200);
        addMember(memberToken, strangerId, ROLE_DEVELOPER).statusCode(201);
    }

    @Test
    public void ownerCanChangeMemberRole() {
        addMember(ownerToken, memberId, ROLE_DEVELOPER).statusCode(201);

        auth(ownerToken).body("{\"roleId\": " + ROLE_MANAGER + "}")
            .put(membersPath() + "/" + memberId).then()
            .statusCode(200)
            .body("roleId", equalTo(ROLE_MANAGER))
            .body("roleName", equalTo("Manager"));
    }

    @Test
    public void ownerCannotBeDemotedOrRemoved() {
        auth(ownerToken).body("{\"roleId\": " + ROLE_DEVELOPER + "}")
            .put(membersPath() + "/" + ownerId).then().statusCode(400);

        auth(ownerToken).delete(membersPath() + "/" + ownerId).then().statusCode(400);
    }

    @Test
    public void removingMemberTakesAwayHisAccess() {
        addMember(ownerToken, memberId, ROLE_DEVELOPER).statusCode(201);
        auth(memberToken).get("/api/projects").then().body("name", hasItem("members-project"));

        auth(ownerToken).delete(membersPath() + "/" + memberId).then().statusCode(204);

        auth(ownerToken).get(membersPath()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].userId", equalTo(ownerId));

        auth(memberToken).get("/api/projects").then()
            .statusCode(200)
            .body("name", not(hasItem("members-project")));
    }

    @Test
    public void changingOrRemovingSomebodyWhoIsNotAMemberGives404() {
        auth(ownerToken).body("{\"roleId\": " + ROLE_MANAGER + "}")
            .put(membersPath() + "/" + strangerId).then().statusCode(404);

        auth(ownerToken).delete(membersPath() + "/" + strangerId).then().statusCode(404);
    }

    @Test
    public void unknownProjectGives404() {
        auth(ownerToken).get("/api/projects/99999/members").then().statusCode(404);
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).get(membersPath()).then().statusCode(401);
    }

    private io.restassured.response.ValidatableResponse addMember(String token, Integer userId, int roleId) {
        return auth(token)
            .body("{\"userId\": " + userId + ", \"roleId\": " + roleId + "}")
            .post(membersPath())
            .then();
    }

    private String membersPath() {
        return "/api/projects/" + projectId + "/members";
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
