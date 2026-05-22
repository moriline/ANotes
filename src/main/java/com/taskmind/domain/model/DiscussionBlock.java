package com.taskmind.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DiscussionBlock(
    UUID id,
    UUID parentId,
    String author,
    BlockType type,
    String content,
    int level,
    Instant createdAt
) {}
