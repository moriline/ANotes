package com.taskmind.api.dto;

import com.taskmind.domain.model.Task;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID projectId,
    String title,
    String description,
    List<String> tags,
    String status,
    Instant createdAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(t.id(), t.projectId(), t.title(), t.description(),
                                t.tags(), t.status().name(), t.createdAt());
    }
}
