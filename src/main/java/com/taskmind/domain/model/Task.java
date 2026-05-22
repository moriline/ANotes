package com.taskmind.domain.model;

import java.time.Instant;
import java.util.List;

public record Task(
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
    List<DiscussionBlock> discussion,
    String summary,
    Instant createdAt,
    Instant updatedAt
) {
    public static Task create(Integer projectId, String title, Integer creatorUserId) {
        return new Task(
            null,
            projectId,
            title,
            null,
            creatorUserId,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            false,
            List.of(),
            null,
            Instant.now(),
            Instant.now()
        );
    }
}
