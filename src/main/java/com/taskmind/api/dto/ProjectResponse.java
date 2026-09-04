package com.taskmind.api.dto;

import com.taskmind.domain.model.Project;
import java.time.Instant;
import java.util.List;

public record ProjectResponse(
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
    public static ProjectResponse fromDomain(Project p) {
        return new ProjectResponse(
            p.id(),
            p.name(),
            p.description(),
            p.ownerUserId(),
            p.color(),
            p.icon(),
            p.tags() != null ? p.tags() : List.of(),
            p.isActive(),
            p.createdAt(),
            p.updatedAt()
        );
    }
}
