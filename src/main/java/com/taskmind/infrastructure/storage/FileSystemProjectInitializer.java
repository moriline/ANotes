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
