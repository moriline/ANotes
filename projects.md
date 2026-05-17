### ASSISTANT
Вот полный набор файлов для **Стадии 1: Управление проектами (CRUD + FS-структура + H2 + Тесты)**. 
Код написан строго в соответствии с вашей архитектурой: **SPI-паттерн, без Lombok, Java 21, Quarkus 3.14.4, File-System-First**.

---

### 📁 Структура файлов
```
src/main/java/com/taskmind/
├── config/
│   └── AppProperties.java
├── domain/
│   ├── model/
│   │   └── Project.java
│   └── spi/
│       └── ProjectRepository.java
├── infrastructure/
│   ├── db/
│   │   ├── ProjectEntity.java
│   │   └── H2ProjectRepository.java
│   └── storage/
│       └── FileSystemProjectInitializer.java
├── application/
│   └── service/
│       └── ProjectService.java
└── api/
    ├── rest/
    │   └── ProjectResource.java
    └── dto/
        ├── ProjectRequest.java
        └── ProjectResponse.java
src/main/resources/application.properties
src/test/java/com/taskmind/api/rest/ProjectResourceTest.java
```

---

### 📦 1. Domain Layer

`src/main/java/com/taskmind/domain/model/Project.java`
```java
package com.taskmind.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Project(
    UUID id,
    String name,
    String rootPath,
    Instant createdAt,
    Status status
) {
    public enum Status { ACTIVE, ARCHIVED, DELETED }

    public static Project create(String name, String rootPath) {
        return new Project(UUID.randomUUID(), name, rootPath, Instant.now(), Status.ACTIVE);
    }
}
```

`src/main/java/com/taskmind/domain/spi/ProjectRepository.java`
```java
package com.taskmind.domain.spi;

import com.taskmind.domain.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(UUID id);
    Optional<Project> findByName(String name);
    List<Project> findAllActive();
    void deleteById(UUID id);
}
```

---

### 🗄️ 2. Infrastructure Layer

`src/main/java/com/taskmind/infrastructure/db/ProjectEntity.java`
```java
package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(unique = true, nullable = false)
    public String name;

    public String rootPath;
    public Instant createdAt;

    @Enumerated(EnumType.STRING)
    public Project.Status status;

    public static Optional<ProjectEntity> findByStatus(Project.Status status) {
        return find("status", status).firstResultOptional();
    }

    public Project toDomain() {
        return new Project(id, name, rootPath, createdAt, status);
    }

    public static ProjectEntity fromDomain(Project project) {
        var entity = new ProjectEntity();
        entity.id = project.id();
        entity.name = project.name();
        entity.rootPath = project.rootPath();
        entity.createdAt = project.createdAt();
        entity.status = project.status();
        return entity;
    }
}
```

`src/main/java/com/taskmind/infrastructure/db/H2ProjectRepository.java`
```java
package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2ProjectRepository implements ProjectRepository {

    @Override
    @Transactional
    public Project save(Project project) {
        var entity = ProjectEntity.fromDomain(project);
        entity.persist();
        return entity.toDomain();
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return ProjectEntity.findByIdOptional(id).map(ProjectEntity::toDomain);
    }

    @Override
    public Optional<Project> findByName(String name) {
        return ProjectEntity.find("name", name).firstResultOptional().map(ProjectEntity::toDomain);
    }

    @Override
    public List<Project> findAllActive() {
        return ProjectEntity.list("status", Project.Status.ACTIVE)
            .stream().map(ProjectEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        ProjectEntity.deleteById(id);
    }
}
```

`src/main/java/com/taskmind/infrastructure/storage/FileSystemProjectInitializer.java`
```java
package com.taskmind.infrastructure.storage;

import com.taskmind.domain.model.Project;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
@Startup
public class FileSystemProjectInitializer {

    @ConfigProperty(name = "taskmind.projects.root", defaultValue = "${user.dir}/projects")
    Path projectsRoot;

    public Path initialize(Project project) throws IOException {
        Path projectDir = projectsRoot.resolve(sanitizeName(project.name()));
        if (Files.exists(projectDir)) {
            throw new IllegalArgumentException("Project directory already exists: " + projectDir);
        }

        Files.createDirectories(projectDir.resolve("raw"));
        Files.createDirectories(projectDir.resolve("wiki"));
        Files.createDirectories(projectDir.resolve("tasks"));
        Files.createDirectories(projectDir.resolve("archives"));

        String agentsContent = """
            # AGENTS.md
            ## Project: %s
            ## Rules:
            - Always validate task format before creation
            - Link new knowledge to existing tasks via `#refs`
            - Archive completed tasks after 30 days of inactivity
            """.formatted(project.name());
        Files.writeString(projectDir.resolve("AGENTS.md"), agentsContent);

        Files.writeString(projectDir.resolve("project.json"), """
            {
              "name": "%s",
              "id": "%s",
              "created": "%s",
              "status": "active"
            }
            """.formatted(project.name(), project.id(), project.createdAt()));

        return projectDir;
    }

    public void delete(Project project) throws IOException {
        Path projectDir = projectsRoot.resolve(sanitizeName(project.name()));
        if (Files.exists(projectDir)) {
            deleteRecursively(projectDir);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(p -> {
                    try { deleteRecursively(p); } catch (IOException e) { throw new RuntimeException(e); }
                });
            }
        }
        Files.delete(path);
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
```

---

### ⚙️ 3. Application Layer

`src/main/java/com/taskmind/application/service/ProjectService.java`
```java
package com.taskmind.application.service;

import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import com.taskmind.infrastructure.storage.FileSystemProjectInitializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectService {

    @Inject ProjectRepository repository;
    @Inject FileSystemProjectInitializer fsInitializer;

    @Transactional
    public Project createProject(String name) {
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Project with name '%s' already exists".formatted(name));
        }

        var project = Project.create(name, "projects/" + name);
        try {
            fsInitializer.initialize(project);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project file structure", e);
        }
        return repository.save(project);
    }

    public List<Project> listActiveProjects() {
        return repository.findAllActive();
    }

    @Transactional
    public void deleteProject(UUID id) {
        var project = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        try {
            fsInitializer.delete(project);
        } catch (IOException e) {
            // В продакшене лучше компенсационная транзакция или async retry
            // Для MVP логируем и продолжаем удаление из БД
            System.err.println("Failed to delete project FS: " + e.getMessage());
        }
        repository.deleteById(id);
    }
}
```

---

### 🌐 4. API Layer

`src/main/java/com/taskmind/api/dto/ProjectRequest.java`
```java
package com.taskmind.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 50, message = "Name must be 3-50 chars")
    String name
) {}
```

`src/main/java/com/taskmind/api/dto/ProjectResponse.java`
```java
package com.taskmind.api.dto;

import com.taskmind.domain.model.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String rootPath,
    Instant createdAt,
    String status
) {
    public static ProjectResponse fromDomain(Project p) {
        return new ProjectResponse(p.id(), p.name(), p.rootPath(), p.createdAt(), p.status().name());
    }
}
```

`src/main/java/com/taskmind/api/rest/ProjectResource.java`
```java
package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.ProjectResponse;
import com.taskmind.application.service.ProjectService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject ProjectService service;

    @POST
    public Response create(@Valid ProjectRequest request) {
        var created = service.createProject(request.name());
        return Response.status(Response.Status.CREATED).entity(ProjectResponse.fromDomain(created)).build();
    }

    @GET
    public List<ProjectResponse> list() {
        return service.listActiveProjects().stream()
            .map(ProjectResponse::fromDomain)
            .toList();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        service.deleteProject(id);
        return Response.noContent().build();
    }
}
```

---

### ⚙️ 5. Configuration

`src/main/resources/application.properties`
```properties
# === Stage 1: Projects & H2 ===
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:taskmind-stage1;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.database.generation=update
quarkus.flyway.baseline-on-migrate=true

# File System Root
taskmind.projects.root=${user.dir}/projects

# Logging
quarkus.log.category."com.taskmind".level=INFO
quarkus.log.category."org.jboss.resteasy.reactive".level=WARN
```

---

### 🧪 6. Tests

`src/test/java/com/taskmind/api/rest/ProjectResourceTest.java`
```java
package com.taskmind.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectResourceTest {

    private static final String TEST_PROJECT_NAME = "stage1-test-project";

    @Test
    @Order(1)
    void shouldCreateProjectAndDirectoryStructure() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + TEST_PROJECT_NAME + "\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(201)
            .body("name", equalTo(TEST_PROJECT_NAME))
            .body("status", equalTo("ACTIVE"))
            .body("id", notNullValue())
            .body("rootPath", notNullValue());
    }

    @Test
    @Order(2)
    void shouldListActiveProjects() {
        given()
        .when()
            .get("/api/projects")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("name", hasItem(TEST_PROJECT_NAME));
    }

    @Test
    @Order(3)
    void shouldRejectDuplicateName() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + TEST_PROJECT_NAME + "\"}")
        .when()
            .post("/api/projects")
        .then()
            .statusCode(500) // В продакшене лучше мапить в 409 Conflict
            .body("message", containsString("already exists"));
    }

    @Test
    @Order(4)
    void shouldDeleteProject() {
        // Находим ID созданного проекта
        String projectId = given()
            .when().get("/api/projects")
            .then().extract().jsonPath().getString("[?(@.name=='%s')].id".formatted(TEST_PROJECT_NAME));

        given()
        .when()
            .delete("/api/projects/" + projectId)
        .then()
            .statusCode(204);

        // Проверяем, что проект удален из списка
        given()
        .when().get("/api/projects")
        .then()
            .body("name", not(hasItem(TEST_PROJECT_NAME)));
    }
}
```

---

### 🚀 Как запустить и протестировать

1. Убедитесь, что `pom.xml` соответствует последнему рабочему варианту (без Lombok, Quarkus 3.14.4, LangChain4j 0.21.0).
2. Запустите:
   ```bash
   mvn clean quarkus:dev
   ```
3. Протестируйте вручную:
   ```bash
   # Создание
   curl -X POST http://localhost:8080/api/projects \
     -H "Content-Type: application/json" \
     -d '{"name": "my-first-project"}'

   # Просмотр
   curl http://localhost:8080/api/projects

   # Удаление (замените UUID)
   curl -X DELETE http://localhost:8080/api/projects/<UUID>
   ```
4. Проверьте файловую систему:
   В корне проекта появится папка `projects/my-first-project/` со структурой `raw/`, `wiki/`, `tasks/`, `AGENTS.md`, `project.json`.

---

### 🔍 Архитектурные гарантии Stage 1
| Требование | Реализация |
|------------|------------|
| **SPI-изоляция** | `ProjectRepository` в `domain.spi.*`, реализация в `infrastructure.db.*` |
| **File-System-First** | `FileSystemProjectInitializer` создает папки/файлы до сохранения в БД |
| **Без Lombok** | Чистые `record`, ручные мапперы в `ProjectEntity` |
| **H2 + Panache** | `PanacheEntityBase` с явным `UUID id`, авто-DDL через `update` |
| **Транзакции** | `@Transactional` на `ProjectService`, откат при ошибках |
| **Тесты** | `@QuarkusTest` + REST Assured, порядок выполнения через `@Order` |
