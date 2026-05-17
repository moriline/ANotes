package com.taskmind.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 50, message = "Name must be 3-50 chars")
    String name,
    String description
) {}
