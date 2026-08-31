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
    @Inject ProjectStatusService projectStatusService;
    @Inject ProjectAccessService projectAccessService;
    @Inject MembershipService membershipService;

    @Transactional
    public Project createProject(String name, String description, Integer ownerUserId) {
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Project with name '%s' already exists".formatted(name));
        }

        var project = Project.create(name, description, ownerUserId);
        var saved = repository.save(project);

        // Без доски по умолчанию задачам проекта было бы некуда вставать.
        projectStatusService.createDefaults(saved.id());

        // Создатель раньше не попадал в projectMembers: формально он не был
        // участником собственного проекта, и роль ему выдать было нечем.
        membershipService.addMember(saved.id(), ownerUserId, MembershipService.ROLE_ADMIN);

        return saved;
    }

    /**
     * Активные проекты, доступные пользователю: свои плюс те, где он участник.
     * Прежний вариант возвращал findAllActive() без всяких проверок, то есть
     * любой залогиненный видел проекты всех остальных.
     */
    public List<Project> listAccessibleProjects(Integer userId) {
        var accessible = projectAccessService.accessibleProjectIds(userId);
        return repository.findAllActive().stream()
            .filter(project -> accessible.contains(project.id()))
            .toList();
    }

    @Transactional
    public void deleteProject(Integer id) {
        repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        repository.deleteById(id);
    }
}
