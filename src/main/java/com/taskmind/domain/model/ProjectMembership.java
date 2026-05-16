package com.taskmind.domain.model;

import java.util.UUID;

public record ProjectMembership(
    UUID userId,
    UUID projectId,
    MembershipRole role
) {
    public enum MembershipRole { OWNER, EDITOR, VIEWER }

    public static ProjectMembership owner(UUID userId, UUID projectId) {
        return new ProjectMembership(userId, projectId, MembershipRole.OWNER);
    }
}
