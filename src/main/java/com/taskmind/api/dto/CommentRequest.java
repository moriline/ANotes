package com.taskmind.api.dto;

import com.taskmind.domain.model.Visibility;
import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
    @NotBlank(message = "Comment content cannot be empty")
    String content,

    /** Необязательно; если не задано — {@link Visibility#PUBLIC}. */
    Visibility visibility
) {}
