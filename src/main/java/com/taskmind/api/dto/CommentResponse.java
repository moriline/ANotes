package com.taskmind.api.dto;

import com.taskmind.domain.model.Visibility;

import java.time.Instant;

public record CommentResponse(
    Integer id,
    Integer taskId,
    Integer userId,
    String content,
    Visibility visibility,
    Instant createdAt,
    Boolean isEdited,
    Instant updatedAt
) {}
