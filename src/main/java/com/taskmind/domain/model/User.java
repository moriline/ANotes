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
    Instant createdAt,
    Instant updatedAt
) {
    public static User register(String username, String email, String password) {
        return new User(null, username, email, password, username, null, true, Instant.now(), Instant.now());
    }
}
