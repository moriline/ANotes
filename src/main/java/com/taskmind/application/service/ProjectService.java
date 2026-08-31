package com.taskmind.application.service;

import com.taskmind.domain.model.ActivityAction;
import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProjectService {

    @Inject ProjectRepository repository;
    @Inject ProjectStatusService projectStatusService;
    @Inject ProjectAccessService projectAccessService;
    @Inject MembershipService membershipService;
    @Inject ActivityLogService activityLog;

    @Transactional
    public Project createProject(String name, String description, Integer ownerUserId) {
        if (repository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Проект с именем '%s' уже существует".formatted(name));
        }

        var project = Project.create(name, description, ownerUserId);
        var saved = repository.save(project);

        // Без доски по умолчанию задачам проекта было бы некуда вставать.
        projectStatusService.createDefaults(saved.id());

        // Событие о создании пишется до добавления владельца, иначе в ленте
        // «участник добавлен» оказывается старше самого проекта.
        var details = new HashMap<String, Object>();
        details.put("name", saved.name());
        activityLog.record(saved.id(), null, ownerUserId, ActivityAction.PROJECT_CREATED, details);

        // Создатель раньше не попадал в projectMembers: формально он не был
        // участником собственного проекта, и роль ему выдать было нечем.
        membershipService.addMember(saved.id(), ownerUserId, MembershipService.ROLE_ADMIN, ownerUserId);

        return saved;
    }

    public Optional<Project> findById(Integer id) {
        return repository.findById(id);
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
