package com.taskmind.api.dto;

import com.taskmind.domain.model.Task;
import java.time.Instant;
import java.util.List;

public record TaskResponse(
    Integer id,
    Integer projectId,
    String title,
    String description,
    Integer creatorUserId,
    Integer assignedUserId,
    Integer statusId,
    Long dueDate,
    Long startDate,
    Double estimatedHours,
    List<String> tags,
    boolean isArchived,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
            t.id(), 
            t.projectId(), 
            t.title(), 
            t.description(),
            t.creatorUserId(),
            t.assignedUserId(),
            t.statusId(),
            t.dueDate(),
            t.startDate(),
            t.estimatedHours(),
            t.tags(), 
            t.isArchived(),
            t.createdAt(),
            t.updatedAt()
        );
    }
}
