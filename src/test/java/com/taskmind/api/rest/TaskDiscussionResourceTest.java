package com.taskmind.api.rest;

import com.taskmind.TestDataCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Обсуждение задачи: {@code GET/POST /api/tasks/{taskId}/discussion}. Сюда
 * пишется ход рассуждений и решения; author — свободная строка, поэтому агент
 * подписывается своим именем.
 */
@QuarkusTest
public class TaskDiscussionResourceTest {

    private static final int ROLE_DEVELOPER = 3;

    @Inject TestDataCleanup cleanup;

    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private String ownerUsername;
    private Integer projectId;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        cleanup.clearAll();
        long ts = System.nanoTime();
        ownerUsername = "dis-owner-" + ts;
        ownerToken = TestAuthHelper.registerAndLogin(ownerUsername, ownerUsername + "@test.com", "Pass123!");
        memberToken = TestAuthHelper.registerAndLogin("dis-member-" + ts, "dis-member-" + ts + "@test.com", "Pass123!");
        strangerToken = TestAuthHelper.registerAndLogin("dis-stranger-" + ts, "dis-stranger-" + ts + "@test.com", "Pass123!");

        projectId = auth(ownerToken).body("{\"name\": \"discussion-project\"}")
            .post("/api/projects").then().statusCode(201)
            .extract().jsonPath().getInt("id");

        auth(ownerToken).body("{\"userId\": " + meId(memberToken) + ", \"roleId\": " + ROLE_DEVELOPER + "}")
            .post("/api/projects/" + projectId + "/members").then().statusCode(201);

        taskId = auth(ownerToken).body("{\"title\": \"Задача с обсуждением\"}")
            .post("/api/projects/" + projectId + "/tasks").then().statusCode(201)
            .extract().jsonPath().getInt("id");
    }

    @Test
    public void discussionStartsEmpty() {
        auth(ownerToken).get(path()).then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    public void blockGetsServerIdCallerAsAuthorAndMessageTypeByDefault() {
        auth(ownerToken).body("{\"content\": \"С чего начать?\"}")
            .post(path()).then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("parentId", nullValue())
            .body("author", equalTo(ownerUsername))
            .body("type", equalTo("MESSAGE"))
            .body("level", equalTo(0))
            .body("content", equalTo("С чего начать?"))
            .body("createdAt", notNullValue());
    }

    @Test
    public void agentCanSignItsOwnConclusionAsDecision() {
        auth(ownerToken)
            .body("{\"author\": \"claude\", \"type\": \"DECISION\", \"content\": \"Кеш инвалидируется по версии сборки\"}")
            .post(path()).then()
            .statusCode(201)
            .body("author", equalTo("claude"))
            .body("type", equalTo("DECISION"));

        auth(ownerToken).get(path()).then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].author", equalTo("claude"))
            .body("[0].type", equalTo("DECISION"));
    }

    @Test
    public void blocksAreReturnedInTheOrderTheyWereAdded() {
        addBlock("первый");
        addBlock("второй");
        addBlock("третий");

        auth(ownerToken).get(path()).then()
            .statusCode(200)
            .body("content", contains("первый", "второй", "третий"));
    }

    @Test
    public void replyIsOneLevelDeeperThanItsParent() {
        String rootId = addBlock("вопрос");

        String childId = auth(ownerToken)
            .body("{\"parentId\": \"" + rootId + "\", \"content\": \"ответ\"}")
            .post(path()).then()
            .statusCode(201)
            .body("level", equalTo(1))
            .body("parentId", equalTo(rootId))
            .extract().jsonPath().getString("id");

        auth(ownerToken)
            .body("{\"parentId\": \"" + childId + "\", \"content\": \"уточнение\"}")
            .post(path()).then()
            .statusCode(201)
            .body("level", equalTo(2));
    }

    @Test
    public void replyToUnknownParentIsRejected() {
        auth(ownerToken)
            .body("{\"parentId\": \"" + UUID.randomUUID() + "\", \"content\": \"в никуда\"}")
            .post(path()).then()
            .statusCode(400);
    }

    @Test
    public void clientGeneratedIdIsPreservedAndCannotBeReused() {
        UUID clientId = UUID.randomUUID();

        auth(ownerToken)
            .body("{\"id\": \"" + clientId + "\", \"content\": \"со своим идентификатором\"}")
            .post(path()).then()
            .statusCode(201)
            .body("id", equalTo(clientId.toString()));

        auth(ownerToken)
            .body("{\"id\": \"" + clientId + "\", \"content\": \"повтор\"}")
            .post(path()).then()
            .statusCode(409);

        auth(ownerToken).get(path()).then().body("$", hasSize(1));
    }

    @Test
    public void blankContentIsRejected() {
        auth(ownerToken).body("{\"content\": \"   \"}").post(path()).then().statusCode(400);
        auth(ownerToken).body("{}").post(path()).then().statusCode(400);
    }

    @Test
    public void memberOfTheProjectCanReadAndWrite() {
        auth(memberToken).body("{\"content\": \"от участника\"}").post(path()).then().statusCode(201);
        auth(memberToken).get(path()).then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    public void strangerCanNeitherReadNorWrite() {
        auth(strangerToken).get(path()).then().statusCode(403);
        auth(strangerToken).body("{\"content\": \"чужое\"}").post(path()).then().statusCode(403);
    }

    @Test
    public void unknownTaskGives404() {
        auth(ownerToken).get("/api/tasks/99999/discussion").then().statusCode(404);
        auth(ownerToken).body("{\"content\": \"x\"}").post("/api/tasks/99999/discussion").then().statusCode(404);
    }

    @Test
    public void unauthenticatedIsRejected() {
        given().contentType(ContentType.JSON).get(path()).then().statusCode(401);
    }

    private String addBlock(String content) {
        return auth(ownerToken).body("{\"content\": \"" + content + "\"}")
            .post(path()).then().statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private String path() {
        return "/api/tasks/" + taskId + "/discussion";
    }

    private int meId(String token) {
        return auth(token).get("/api/users/me").then().statusCode(200)
            .extract().jsonPath().getInt("id");
    }

    private static RequestSpecification auth(String token) {
        return TestAuthHelper.authenticated(token);
    }
}
