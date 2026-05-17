package com.taskmind.domain.model;

public record ProjectMembership(
    Integer id,
    Integer projectId,
    Integer userId,
    Integer roleId,
    Long joinedAt
) {
    public static ProjectMembership create(Integer projectId, Integer userId, Integer roleId) {
        return new ProjectMembership(null, projectId, userId, roleId, System.currentTimeMillis());
    }
}
