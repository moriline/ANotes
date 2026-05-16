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
            System.err.println("Failed to delete project FS: " + e.getMessage());
        }
        repository.deleteById(id);
    }
}
