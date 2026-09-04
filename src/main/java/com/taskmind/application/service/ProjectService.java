package com.taskmind.application.service;

import com.taskmind.api.dto.ProjectUpdateRequest;
import com.taskmind.domain.model.ActivityAction;
import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class ProjectService {

    @Inject ProjectRepository repository;
    @Inject ProjectStatusService projectStatusService;
    @Inject ProjectAccessService projectAccessService;
    @Inject MembershipService membershipService;
    @Inject ActivityLogService activityLog;

    @Transactional
    public Project createProject(String name, String description, String color, String icon,
                                 List<String> tags, Integer ownerUserId) {
        if (repository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Проект с именем '%s' уже существует".formatted(name));
        }

        var project = Project.create(name, description, color, icon, tags, ownerUserId);
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
     * Частичное обновление проекта: имя, описание, цвет, иконку, тэги. Поле со
     * значением {@code null} не трогается; тэги заменяются целиком (пустой список
     * очищает их). Смена имени на занятое — 409.
     */
    @Transactional
    public Project updateProject(Project current, ProjectUpdateRequest req, Integer actorUserId) {
        if (req.name() != null && !req.name().equals(current.name())
            && repository.findByName(req.name()).isPresent()) {
            throw new DuplicateResourceException(
                "Проект с именем '%s' уже существует".formatted(req.name()));
        }

        var updated = new Project(
            current.id(),
            req.name() != null ? req.name() : current.name(),
            req.description() != null ? req.description() : current.description(),
            current.ownerUserId(),
            req.color() != null ? req.color() : current.color(),
            req.icon() != null ? req.icon() : current.icon(),
            req.tags() != null ? List.copyOf(req.tags()) : current.tags(),
            current.isActive(),
            current.createdAt(),
            Instant.now()
        );
        var saved = repository.save(updated);

        var changed = new ArrayList<String>();
        if (!Objects.equals(current.name(), saved.name())) changed.add("name");
        if (!Objects.equals(current.description(), saved.description())) changed.add("description");
        if (!Objects.equals(current.color(), saved.color())) changed.add("color");
        if (!Objects.equals(current.icon(), saved.icon())) changed.add("icon");
        if (!Objects.equals(current.tags(), saved.tags())) changed.add("tags");
        if (!changed.isEmpty()) {
            var details = new HashMap<String, Object>();
            details.put("fields", changed);
            activityLog.record(saved.id(), null, actorUserId, ActivityAction.PROJECT_UPDATED, details);
        }
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
