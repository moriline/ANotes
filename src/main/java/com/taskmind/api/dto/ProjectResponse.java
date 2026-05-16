package com.taskmind.api.dto;

import com.taskmind.domain.model.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String rootPath,
    Instant createdAt,
    String status
) {
    public static ProjectResponse fromDomain(Project p) {
        return new ProjectResponse(p.id(), p.name(), p.rootPath(), p.createdAt(), p.status().name());
    }
}
