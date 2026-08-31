package com.taskmind.domain.model;

import java.time.Instant;

public record User(
    Integer id,
    String username,
    String email,
    String password,
    String displayName,
    String avatarUrl,
    boolean isActive,
    /**
     * Глобальный администратор. Роли из {@code projectRoles} действуют только
     * внутри одного проекта, поэтому управление пользователями ими не выразить:
     * иначе владелец любого проекта смог бы удалять чужие учётные записи.
     */
    boolean isAdmin,
    Instant createdAt,
    Instant updatedAt
) {
    public static User register(String username, String email, String password) {
        return new User(null, username, email, password, username, null, true, false, Instant.now(), Instant.now());
    }
}
