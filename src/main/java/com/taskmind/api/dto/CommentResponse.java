package com.taskmind.api.dto;

import java.time.Instant;

public record CommentResponse(
    Integer id,
    Integer taskId,
    Integer userId,
    String content,
    Instant createdAt,
    Boolean isEdited,
    Instant updatedAt
) {}
