package com.taskmind.api.dto;

import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.model.User;

/**
 * Участник проекта вместе с именем пользователя и названием роли — чтобы список
 * участников можно было показать, не делая запрос на каждого человека отдельно.
 */
public record ProjectMemberResponse(
    Integer id,
    Integer projectId,
    Integer userId,
    String username,
    String displayName,
    Integer roleId,
    String roleName,
    Long joinedAt
) {
    public static ProjectMemberResponse of(ProjectMembership membership, User user, String roleName) {
        return new ProjectMemberResponse(
            membership.id(),
            membership.projectId(),
            membership.userId(),
            user != null ? user.username() : null,
            user != null ? user.displayName() : null,
            membership.roleId(),
            roleName,
            membership.joinedAt()
        );
    }
}
