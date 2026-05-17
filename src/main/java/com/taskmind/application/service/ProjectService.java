package com.taskmind.application.service;

import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ProjectService {

    @Inject ProjectRepository repository;

    @Transactional
    public Project createProject(String name, String description, Integer ownerUserId) {
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Project with name '%s' already exists".formatted(name));
        }

        var project = Project.create(name, description, ownerUserId);
        return repository.save(project);
    }

    public List<Project> listActiveProjects() {
        return repository.findAllActive();
    }

    @Transactional
    public void deleteProject(Integer id) {
        repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        repository.deleteById(id);
    }
}
