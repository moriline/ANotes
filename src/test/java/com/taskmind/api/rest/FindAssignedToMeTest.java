package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.ProjectMemberRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskUpdateRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code assignedToMe} в /api/find — «покажи мои задачи» без предварительного
 * запроса своего id.
 */
@QuarkusTest
public class FindAssignedToMeTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private Integer ownerId;
    private Integer memberId;
    private Integer projectId;
    private Integer secondProjectId;
    private Integer mineId;
    private Integer ownersId;
    private Integer unassignedId;
    private Integer mineInSecondProjectId;
    private Integer inProgressStatusId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerToken = TestAuthHelper.registerAndLogin("mine-owner-" + ts, "mine-owner-" + ts + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("mine-member-" + ts, "mine-member-" + ts + "@test.com", "Pass123!");

        ownerId = meId(ownerToken);
        memberId = meId(memberToken);

        projectId = createProject("assigned-project");
        secondProjectId = createProject("assigned-second-project");
        addMember(projectId, memberId);
        addMember(secondProjectId, memberId);

        mineId = createTask(projectId, "моя задача");
        ownersId = createTask(projectId, "задача владельца");
        unassignedId = createTask(projectId, "ничья задача");
        mineInSecondProjectId = createTask(secondProjectId, "моя во втором проекте");

        inProgressStatusId = auth(ownerToken).get("/api/project-statuses/project/" + projectId)
            .then().statusCode(200).extract().jsonPath().getInt("[1].id");

        assign(projectId, mineId, memberId);
        assign(projectId, ownersId, ownerId);
        assign(secondProjectId, mineInSecondProjectId, memberId);
    }

    @Test
    public void returnsOnlyTasksAssignedToTheCaller() {
        List<Integer> mine = idsOf(find(memberToken, assignedToMe(true)));

        assertTrue(mine.containsAll(List.of(mineId, mineInSecondProjectId)),
            "должны быть обе мои задачи из обоих проектов: " + mine);
        assertFalse(mine.contains(ownersId), "чужая назначенная задача не наша");
        assertFalse(mine.contains(unassignedId), "неназначенная задача не наша");
    }

    @Test
    public void ownerSeesADifferentSetThanTheMember() {
        assertEquals(List.of(ownersId), idsOf(find(ownerToken, assignedToMe(true))));
    }

    @Test
    public void combinesWithOtherFilters() {
        auth(memberToken).body(new TaskUpdateRequest(null, null, null, null, inProgressStatusId, null, null, null, null))
            .patch("/api/projects/" + projectId + "/tasks/" + mineId).then().statusCode(200);

        FindTasksRequest byStatus = assignedToMe(true);
        byStatus.statusId = inProgressStatusId;
        assertEquals(List.of(mineId), idsOf(find(memberToken, byStatus)));

        FindTasksRequest byProject = assignedToMe(true);
        byProject.projectId = projectId;
        assertEquals(List.of(mineId), idsOf(find(memberToken, byProject)));
    }

    @Test
    public void falseAndAbsentBehaveTheSame() {
        List<Integer> withFalse = idsOf(find(memberToken, assignedToMe(false)));
        List<Integer> withoutIt = idsOf(find(memberToken, new FindTasksRequest()));

        assertEquals(withoutIt, withFalse);
        assertTrue(withFalse.contains(ownersId), "без фильтра видны все доступные задачи");
    }

    @Test
    public void explicitAssignedUserIdWithTheSameUserIsAccepted() {
        FindTasksRequest withExplicitId = assignedToMe(true);
        withExplicitId.assignedUserId = memberId;

        assertEquals(idsOf(find(memberToken, assignedToMe(true))),
            idsOf(find(memberToken, withExplicitId)));
    }

    @Test
    public void contradictingAssignedUserIdIsRejected() {
        FindTasksRequest contradicting = assignedToMe(true);
        contradicting.assignedUserId = ownerId;

        auth(memberToken).body(contradicting)
            .post("/api/find").then().statusCode(400);
    }

    @Test
    public void totalReflectsTheFilter() {
        JsonPath response = find(memberToken, assignedToMe(true));
        assertEquals(2, response.getLong("total"));
    }

    private void assign(Integer project, Integer taskId, Integer userId) {
        auth(ownerToken).body(new TaskUpdateRequest(null, null, null, userId, null, null, null, null, null))
            .patch("/api/projects/" + project + "/tasks/" + taskId).then().statusCode(200);
    }

    private void addMember(Integer project, Integer userId) {
        auth(ownerToken).body(new ProjectMemberRequest(userId, ROLE_DEVELOPER))
            .post("/api/projects/" + project + "/members").then().statusCode(201);
    }

    private Integer createProject(String name) {
        return auth(ownerToken).body(new ProjectRequest(name, null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    private Integer createTask(Integer project, String title) {
        return auth(ownerToken).body(new TaskRequest(title, null, null))
            .post("/api/projects/" + project + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    /** {@code assignedToMe}-фильтр, к которому тесты дописывают остальные поля. */
    private static FindTasksRequest assignedToMe(Boolean value) {
        FindTasksRequest query = new FindTasksRequest();
        query.assignedToMe = value;
        return query;
    }

    private JsonPath find(String token, FindTasksRequest body) {
        return auth(token).body(body).post("/api/find").then().statusCode(200).extract().jsonPath();
    }

    private static List<Integer> idsOf(JsonPath response) {
        return response.getList("tasks.id", Integer.class);
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
