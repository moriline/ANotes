package com.taskmind.domain.model;

import java.time.Instant;

public record Project(
    Integer id,
    String name,
    String description,
    Integer ownerUserId,
    String color,
    String icon,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public static Project create(String name, String description, Integer ownerUserId) {
        return new Project(null, name, description, ownerUserId, "#4A90D9", null, true, Instant.now(), Instant.now());
    }
}
