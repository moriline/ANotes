package com.taskmind.domain.model;

import java.time.Instant;
import java.util.List;

public record Project(
    Integer id,
    String name,
    String description,
    Integer ownerUserId,
    String color,
    String icon,
    List<String> tags,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public static final String DEFAULT_COLOR = "#4A90D9";

    public static Project create(String name, String description, String color, String icon,
                                 List<String> tags, Integer ownerUserId) {
        Instant now = Instant.now();
        return new Project(
            null,
            name,
            description,
            ownerUserId,
            color != null && !color.isBlank() ? color : DEFAULT_COLOR,
            icon,
            tags != null ? List.copyOf(tags) : List.of(),
            true,
            now,
            now
        );
    }
}
