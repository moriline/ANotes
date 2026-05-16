package com.taskmind.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record TaskRequest(
    @NotBlank(message = "Task title is required")
    String title,
    String description,
    List<String> tags
) {}
