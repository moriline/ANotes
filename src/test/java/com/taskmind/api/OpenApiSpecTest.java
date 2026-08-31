package com.taskmind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Сторож против расхождения спеки и кода.
 *
 * <p>Четыре прежних рукописных yaml разошлись с кодом молча: вместе они описывали
 * 32 пути из 43 существующих, пять из них выдуманные. Спека теперь генерируется,
 * но снимок в репозитории отстать по-прежнему может — эти тесты роняют сборку,
 * если он отстал.
 *
 * <p>Сверяется набор операций «метод + путь» и требование авторизации у каждой.
 * Схемы тел намеренно не сверяются: тест ловит появление и пропажу ручек, а не
 * каждое добавленное поле в DTO.
 */
@QuarkusTest
public class OpenApiSpecTest {

    /** Снимок в корне репозитория; surefire стартует из него же. */
    private static final Path SNAPSHOT = Path.of("openapi5.yaml");

    /**
     * Единственные ручки без {@code @RolesAllowed}. Регистрация и вход открыты по
     * смыслу, а две AI-ручки — отладочные, и держать их открытыми, скорее всего,
     * не следует (записано в todo.txt).
     */
    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
        "POST /api/auth/register",
        "POST /api/auth/login",
        "GET /config/ai",
        "GET /test/ai/embed");

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    public void specIsServedAndCarriesOurInfoBlock() {
        given().get("/q/openapi").then().statusCode(200);

        JsonNode info = liveSpec().get("info");
        assertEquals("TaskMind API", info.get("title").asText());
        assertEquals("5.0.0", info.get("version").asText());
    }

    /**
     * Главное: снимок в репозитории описывает ровно те же операции, что живая
     * спека. Разошлось — перегенерировать по инструкции в шапке openapi5.yaml.
     */
    @Test
    public void snapshotDescribesExactlyTheLiveOperations() throws IOException {
        Set<String> live = operations(liveSpec());
        Set<String> snapshot = operations(snapshotSpec());

        assertEquals(snapshot, live,
            "openapi5.yaml разошёлся с /q/openapi — перегенерируйте снимок");
        assertFalse(live.isEmpty(), "живая спека не отдала ни одной операции");
    }

    /** Ручка, забывшая @RolesAllowed, станет видна здесь, а не в проде. */
    @Test
    public void everyOperationOutsideTheKnownPublicOnesRequiresBearerAuth() {
        JsonNode spec = liveSpec();
        Set<String> unprotected = new TreeSet<>();

        for (String operation : operations(spec)) {
            if (!requiresBearerAuth(spec, operation)) {
                unprotected.add(operation);
            }
        }

        assertEquals(PUBLIC_OPERATIONS, unprotected,
            "изменился список ручек без авторизации");
    }

    @Test
    public void bearerSchemeIsDeclaredSoAGeneratedClientSendsTheToken() {
        JsonNode scheme = liveSpec().at("/components/securitySchemes/bearerAuth");

        assertFalse(scheme.isMissingNode(), "схема bearerAuth не объявлена");
        assertEquals("http", scheme.get("type").asText());
        assertEquals("bearer", scheme.get("scheme").asText());
        assertEquals("JWT", scheme.get("bearerFormat").asText());
    }

    // --- helpers ------------------------------------------------------------

    private JsonNode liveSpec() {
        String body = given().get("/q/openapi?format=json").then()
            .statusCode(200)
            .extract().body().asString();
        try {
            return YAML.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("не удалось разобрать ответ /q/openapi", e);
        }
    }

    private JsonNode snapshotSpec() throws IOException {
        assertTrue(Files.exists(SNAPSHOT), "нет файла " + SNAPSHOT.toAbsolutePath());
        return YAML.readTree(Files.readString(SNAPSHOT));
    }

    /** Множество строк вида «GET /api/projects/{id}». */
    private static Set<String> operations(JsonNode spec) {
        Set<String> result = new TreeSet<>();
        JsonNode paths = spec.get("paths");
        assertNotNull(paths, "в спеке нет секции paths");

        for (Iterator<Map.Entry<String, JsonNode>> it = paths.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> path = it.next();
            for (Iterator<String> verbs = path.getValue().fieldNames(); verbs.hasNext(); ) {
                String verb = verbs.next();
                // На уровне пути кроме методов лежат parameters, summary и прочее.
                if (isHttpVerb(verb)) {
                    result.add(verb.toUpperCase() + " " + path.getKey());
                }
            }
        }
        return result;
    }

    private static boolean isHttpVerb(String name) {
        return switch (name) {
            case "get", "post", "put", "delete", "patch", "head", "options", "trace" -> true;
            default -> false;
        };
    }

    private static boolean requiresBearerAuth(JsonNode spec, String operation) {
        String[] parts = operation.split(" ", 2);
        JsonNode security = spec.get("paths").get(parts[1]).get(parts[0].toLowerCase()).get("security");
        if (security == null) {
            return false;
        }
        for (JsonNode requirement : security) {
            if (requirement.has("bearerAuth")) {
                return true;
            }
        }
        return false;
    }
}
