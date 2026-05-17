package com.taskmind.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
    @NotBlank(message = "Comment content cannot be empty")
    String content
) {}
