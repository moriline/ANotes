package com.taskmind.application.service;

import com.taskmind.domain.model.Action;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.model.ProjectRole;
import com.taskmind.domain.spi.MembershipRepository;
import com.taskmind.domain.spi.ProjectRepository;
import com.taskmind.infrastructure.db.ProjectRoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

@ApplicationScoped
public class PermissionService {

    @Inject
    MembershipRepository membershipRepository;

    @Inject
    ProjectRepository projectRepository;

    private static final Map<String, List<String>> HIERARCHY = Map.of(
        "Admin", List.of("Manager", "Developer", "Guest"),
        "Manager", List.of("Developer", "Guest"),
        "Developer", List.of("Guest"),
        "Guest", List.of()
    );

    public boolean hasPermission(Integer userId, Integer projectId, String actionValue) {
        try {
            Action action = Action.fromValue(actionValue);
            return hasPermission(userId, projectId, action);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Право на действие в проекте с поправкой на владельца.
     *
     * <p>{@link #hasPermission} смотрит только в projectMembers, а владелец
     * проекта, заведённого до того, как создатель стал автоматически попадать в
     * участники, там отсутствует — и оказался бы без прав на собственный проект.
     */
    public boolean canPerform(Integer userId, Integer projectId, Action action) {
        boolean isOwner = projectRepository.findById(projectId)
            .map(project -> userId.equals(project.ownerUserId()))
            .orElse(false);
        return isOwner || hasPermission(userId, projectId, action);
    }

    public boolean hasPermission(Integer userId, Integer projectId, Action action) {
        Optional<ProjectMembership> membership = membershipRepository.findByUserAndProject(userId, projectId);
        if (membership.isEmpty()) {
            return false;
        }

        Integer roleId = membership.get().roleId();
        return checkRoleAndHierarchy(roleId, action, new HashSet<>());
    }

    /**
     * Видит ли пользователь внутренний контент проекта — {@code INTERNAL}/{@code SYSTEM}
     * комментарии и события ленты. Не видит его только заказчик ({@link MembershipService#ROLE_CLIENT}):
     * он внешний, ему доступна лишь публичная переписка. Владелец проекта и все
     * командные роли (в т. ч. Guest) видят всё; пользователь без членства сюда не
     * доходит — доступ к проекту проверяется раньше, поэтому дефолт {@code true}.
     */
    public boolean seesInternalContent(Integer userId, Integer projectId) {
        boolean isOwner = projectRepository.findById(projectId)
            .map(project -> userId.equals(project.ownerUserId()))
            .orElse(false);
        if (isOwner) {
            return true;
        }
        return membershipRepository.findByUserAndProject(userId, projectId)
            .map(membership -> membership.roleId() != MembershipService.ROLE_CLIENT)
            .orElse(true);
    }

    private boolean checkRoleAndHierarchy(Integer roleId, Action action, Set<Integer> visited) {
        if (visited.contains(roleId)) return false;
        visited.add(roleId);

        ProjectRoleEntity roleEntity = ProjectRoleEntity.findById(roleId);
        if (roleEntity == null) return false;

        if (roleEntity.permissions.contains(action)) {
            return true;
        }

        // Check children roles in hierarchy
        List<String> childrenNames = HIERARCHY.getOrDefault(roleEntity.roleName, List.of());
        for (String childName : childrenNames) {
            ProjectRoleEntity childEntity = ProjectRoleEntity.find("roleName", childName).firstResult();
            if (childEntity != null && checkRoleAndHierarchy(childEntity.id, action, visited)) {
                return true;
            }
        }

        return false;
    }
}
