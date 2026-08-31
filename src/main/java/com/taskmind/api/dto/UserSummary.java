package com.taskmind.api.dto;

import com.taskmind.domain.model.User;

/**
 * Пользователь в справочнике: этого достаточно, чтобы выбрать исполнителя или
 * найти, кого добавить в проект.
 *
 * <p>Email сюда намеренно не входит: искать по нему можно, а вот отдавать почту
 * всех пользователей каждому залогиненному незачем. Свою почту владелец видит
 * в {@code GET /api/users/me}.
 */
public record UserSummary(
    Integer id,
    String username,
    String displayName,
    String avatarUrl
) {
    public static UserSummary from(User user) {
        return new UserSummary(user.id(), user.username(), user.displayName(), user.avatarUrl());
    }
}
