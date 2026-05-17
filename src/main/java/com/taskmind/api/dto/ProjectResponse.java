package com.taskmind.api.dto;

import com.taskmind.domain.model.Project;
import java.time.Instant;

public record ProjectResponse(
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
    public static ProjectResponse fromDomain(Project p) {
        return new ProjectResponse(
            p.id(), 
            p.name(), 
            p.description(), 
            p.ownerUserId(), 
            p.color(), 
            p.icon(), 
            p.isActive(), 
            p.createdAt(), 
            p.updatedAt()
        );
    }
}
