package com.taskmind.infrastructure.storage;

import com.taskmind.domain.model.Task;
import com.taskmind.infrastructure.db.ProjectEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
@Startup
public class FileSystemTaskSync {

    private final ObjectMapper mapper;
    private final Path projectsRoot;

    public FileSystemTaskSync(@ConfigProperty(name = "taskmind.projects.root", defaultValue = "${user.dir}/projects") Path root) {
        this.projectsRoot = root;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public Path syncToDisk(Task task) throws IOException {
        var project = ProjectEntity.<ProjectEntity>find("id", task.projectId()).firstResult();
        if (project == null) throw new IllegalArgumentException("Project not found: " + task.projectId());

        Path taskDir = projectsRoot.resolve(sanitize(project.name)).resolve("tasks");
        Files.createDirectories(taskDir);
        Path taskFile = taskDir.resolve(task.id() + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(taskFile.toFile(), task);
        return taskFile;
    }

    public void deleteFromDisk(Task task) throws IOException {
        var project = ProjectEntity.<ProjectEntity>find("id", task.projectId()).firstResult();
        if (project == null) return;
        Path taskFile = projectsRoot.resolve(sanitize(project.name))
                                    .resolve("tasks")
                                    .resolve(task.id() + ".json");
        Files.deleteIfExists(taskFile);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
