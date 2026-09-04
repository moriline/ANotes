package com.taskmind.application.service;

import com.taskmind.domain.model.ActivityAction;
import com.taskmind.domain.model.Project;
import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import com.taskmind.domain.spi.ProjectRepository;
import com.taskmind.infrastructure.db.ProjectRoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Состав участников проекта.
 *
 * <p>До этого таблица {@code projectMembers} заполнялась только сидом: REST-ручек
 * не было, а создатель проекта в участники не попадал. Из-за этого вся ролевая
 * модель существовала на бумаге — выдать кому-то роль в проекте через API было
 * невозможно.
 */
@ApplicationScoped
public class MembershipService {

    /** roleId роли Admin из справочника projectRoles. */
    public static final int ROLE_ADMIN = 1;

    /**
     * roleId роли Client (заказчик). Внешняя роль: по правам как Guest, но код
     * прячет от неё внутренний контент — см. {@link PermissionService#seesInternalContent}.
     */
    public static final int ROLE_CLIENT = 6;

    @Inject MembershipRepository membershipRepository;
    @Inject ProjectRepository projectRepository;
    @Inject ActivityLogService activityLog;

    public List<ProjectMembership> listMembers(Integer projectId) {
        return membershipRepository.findByProject(projectId);
    }

    public Optional<ProjectMembership> find(Integer projectId, Integer userId) {
        return membershipRepository.findByUserAndProject(userId, projectId);
    }

    public boolean roleExists(Integer roleId) {
        return roleId != null && ProjectRoleEntity.findById(roleId) != null;
    }

    public String roleName(Integer roleId) {
        ProjectRoleEntity role = ProjectRoleEntity.findById(roleId);
        return role != null ? role.roleName : null;
    }

    /**
     * Управлять составом может владелец проекта или участник с ролью Admin.
     * Владелец проверяется отдельно ради проектов, заведённых до того, как
     * создатель стал автоматически попадать в участники.
     */
    public boolean canManageMembers(Integer userId, Integer projectId) {
        boolean isOwner = projectRepository.findById(projectId)
            .map(project -> userId.equals(project.ownerUserId()))
            .orElse(false);
        if (isOwner) {
            return true;
        }
        return find(projectId, userId)
            .map(membership -> membership.roleId() == ROLE_ADMIN)
            .orElse(false);
    }

    public Optional<Project> findProject(Integer projectId) {
        return projectRepository.findById(projectId);
    }

    @Transactional
    public ProjectMembership addMember(Integer projectId, Integer userId, Integer roleId, Integer actorUserId) {
        var saved = membershipRepository.save(ProjectMembership.create(projectId, userId, roleId));

        var details = new HashMap<String, Object>();
        details.put("userId", userId);
        details.put("roleId", roleId);
        details.put("roleName", roleName(roleId));
        activityLog.record(projectId, null, actorUserId, ActivityAction.MEMBER_ADDED, details);

        return saved;
    }

    @Transactional
    public ProjectMembership changeRole(ProjectMembership membership, Integer roleId, Integer actorUserId) {
        var saved = membershipRepository.save(new ProjectMembership(
            membership.id(),
            membership.projectId(),
            membership.userId(),
            roleId,
            membership.joinedAt()
        ));

        var details = new HashMap<String, Object>();
        details.put("userId", membership.userId());
        details.put("fromRoleId", membership.roleId());
        details.put("toRoleId", roleId);
        details.put("roleName", roleName(roleId));
        activityLog.record(membership.projectId(), null, actorUserId, ActivityAction.MEMBER_ROLE_CHANGED, details);

        return saved;
    }

    @Transactional
    public void removeMember(ProjectMembership membership, Integer actorUserId) {
        membershipRepository.deleteById(membership.id());

        var details = new HashMap<String, Object>();
        details.put("userId", membership.userId());
        details.put("roleId", membership.roleId());
        activityLog.record(membership.projectId(), null, actorUserId, ActivityAction.MEMBER_REMOVED, details);
    }
}
