package com.taskmind.api.dto;

import com.taskmind.infrastructure.db.UserEntity;

/**
 * Пользователь в административном списке.
 *
 * <p>Раньше {@code GET /api/admin/users} отдавал {@code List<UserEntity>} целиком,
 * то есть вместе с полем {@code password} — bcrypt-хешами всех учётных записей.
 * DTO существует именно для того, чтобы хеш физически неоткуда было взять.
 */
public record AdminUserResponse(
    Integer id,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    boolean isActive,
    boolean isAdmin,
    Long createdAt,
    Long updatedAt
) {
    public static AdminUserResponse from(UserEntity user) {
        return new AdminUserResponse(
            user.id,
            user.username,
            user.email,
            user.displayName,
            user.avatarUrl,
            user.isActive,
            user.isAdmin,
            user.createdAt,
            user.updatedAt
        );
    }
}
