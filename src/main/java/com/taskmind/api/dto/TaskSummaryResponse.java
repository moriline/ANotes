package com.taskmind.api.dto;

import com.taskmind.domain.model.Task;
import java.time.Instant;

public record TaskSummaryResponse(
    Integer taskId,
    String summary,
    Instant updatedAt
) {
    public static TaskSummaryResponse from(Task task) {
        return new TaskSummaryResponse(task.id(), task.summary(), task.updatedAt());
    }
}
