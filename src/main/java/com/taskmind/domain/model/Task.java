package com.taskmind.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Task(
    UUID id,
    UUID projectId,
    String title,
    String description,
    List<String> tags,
    Status status,
    Instant createdAt
) {
    public enum Status { TODO, IN_PROGRESS, DONE }

    public static Task create(UUID projectId, String title, String description, List<String> tags) {
        return new Task(
            UUID.randomUUID(),
            projectId,
            title,
            description != null ? description : "",
            tags != null ? tags : List.of(),
            Status.TODO,
            Instant.now()
        );
    }
}
