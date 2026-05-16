package com.taskmind.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    Set<Role> roles,
    Instant createdAt
) {
    public enum Role { USER, ADMIN }

    public static User register(String username, String email, String passwordHash) {
        return new User(UUID.randomUUID(), username, email, passwordHash, Set.of(Role.USER), Instant.now());
    }
}
