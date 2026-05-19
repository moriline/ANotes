package com.taskmind.api.dto;

import com.taskmind.domain.model.Action;

public record PermissionCheckRequest(
    Integer userId,
    Integer projectId,
    String action
) {}
