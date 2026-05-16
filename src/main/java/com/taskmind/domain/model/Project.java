package com.taskmind.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Project(
    UUID id,
    String name,
    String rootPath,
    Instant createdAt,
    Status status
) {
    public enum Status { ACTIVE, ARCHIVED, DELETED }

    public static Project create(String name, String rootPath) {
        return new Project(UUID.randomUUID(), name, rootPath, Instant.now(), Status.ACTIVE);
    }
}
