package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import com.taskmind.api.dto.DiscussionBlockRequest;
import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.TaskRequest;
import com.taskmind.domain.model.BlockType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * {@code PUT /api/tasks/{taskId}/discussion} — замена дерева обсуждения целиком:
 * правка формулировок, перевешивание веток, удаление лишнего.
 */
@QuarkusTest
public class TaskDiscussionReplaceTest {

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String strangerToken;
    private String ownerUsername;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerUsername = "put-owner-" + ts;
        ownerToken = TestAuthHelper.registerAndLogin(ownerUsername, ownerUsername + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("put-stranger-" + ts, "put-stranger-" + ts + "@test.com", "Pass123!");

        Integer projectId = auth(ownerToken).body(new ProjectRequest("put-discussion-project", null))
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        taskId = auth(ownerToken).body(new TaskRequest("Задача с деревом", null, null))
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void replacesTheWholeTree() {
        auth(ownerToken).body(block("старое рассуждение")).post(path()).then().statusCode(201);
        auth(ownerToken).body(block("ещё одно")).post(path()).then().statusCode(201);

        auth(ownerToken).body(List.of(block("единственный уцелевший блок")))
            .put(path()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].content", equalTo("единственный уцелевший блок"));

        auth(ownerToken).get(path()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].content", equalTo("единственный уцелевший блок"));
    }

    @Test
    public void recomputesLevelsFromParentLinks() {
        UUID root = UUID.randomUUID();
        UUID child = UUID.randomUUID();

        auth(ownerToken).body(List.of(
                new DiscussionBlockRequest(root, null, null, null, "вопрос"),
                new DiscussionBlockRequest(child, root, null, null, "ответ"),
                new DiscussionBlockRequest(null, child, null, null, "уточнение")))
            .put(path()).then()
            .statusCode(200)
            .body("level", contains(0, 1, 2))
            .body("[2].id", notNullValue());
    }

    @Test
    public void keepsCreationTimeOfBlocksThatAlreadyExisted() {
        var created = auth(ownerToken).body(block("первая редакция"))
            .post(path()).then().statusCode(201).extract().jsonPath();
        UUID blockId = UUID.fromString(created.getString("id"));
        String createdAt = created.getString("createdAt");

        auth(ownerToken).body(List.of(new DiscussionBlockRequest(blockId, null, null, null, "вторая редакция")))
            .put(path()).then()
            .statusCode(200)
            .body("[0].content", equalTo("вторая редакция"))
            .body("[0].createdAt", equalTo(createdAt));
    }

    @Test
    public void emptyListClearsTheDiscussion() {
        auth(ownerToken).body(block("будет стёрто")).post(path()).then().statusCode(201);

        auth(ownerToken).body(List.of()).put(path()).then().statusCode(200).body("$", hasSize(0));
        auth(ownerToken).get(path()).then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    public void authorAndTypeAreKeptOrDefaulted() {
        auth(ownerToken).body(List.of(
                new DiscussionBlockRequest(null, null, "claude", BlockType.DECISION, "вывод модели"),
                block("без подписи")))
            .put(path()).then()
            .statusCode(200)
            .body("[0].author", equalTo("claude"))
            .body("[0].type", equalTo("DECISION"))
            .body("[1].author", equalTo(ownerUsername))
            .body("[1].type", equalTo("MESSAGE"));
    }

    @Test
    public void duplicateIdsAreRejected() {
        UUID duplicate = UUID.randomUUID();
        auth(ownerToken).body(List.of(
                new DiscussionBlockRequest(duplicate, null, null, null, "раз"),
                new DiscussionBlockRequest(duplicate, null, null, null, "два")))
            .put(path()).then().statusCode(400);
    }

    @Test
    public void parentOutsideTheSubmittedTreeIsRejected() {
        auth(ownerToken).body(List.of(new DiscussionBlockRequest(null, UUID.randomUUID(), null, null, "сирота")))
            .put(path()).then().statusCode(400);
    }

    @Test
    public void cycleIsRejected() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        auth(ownerToken).body(List.of(
                new DiscussionBlockRequest(first, second, null, null, "a"),
                new DiscussionBlockRequest(second, first, null, null, "b")))
            .put(path()).then().statusCode(400);
    }

    @Test
    public void blankContentIsRejected() {
        auth(ownerToken).body(List.of(block("   "))).put(path()).then().statusCode(400);
    }

    @Test
    public void failedReplaceLeavesTheOldTreeIntact() {
        auth(ownerToken).body(block("должно уцелеть")).post(path()).then().statusCode(201);

        auth(ownerToken).body(List.of(block(""))).put(path()).then().statusCode(400);

        auth(ownerToken).get(path()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].content", equalTo("должно уцелеть"));
    }

    @Test
    public void strangerCannotReplace() {
        auth(strangerToken).body(List.of(block("чужое"))).put(path()).then().statusCode(403);
    }

    @Test
    public void unknownTaskGives404() {
        auth(ownerToken).body(List.of(block("x"))).put("/api/tasks/99999/discussion").then().statusCode(404);
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).body(List.of()).put(path()).then().statusCode(401);
    }

    private static DiscussionBlockRequest block(String content) {
        return new DiscussionBlockRequest(null, null, null, null, content);
    }

    private String path() {
        return "/api/tasks/" + taskId + "/discussion";
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
