package com.taskmind.api.dto;

import jakarta.validation.constraints.NotNull;

/** Смена роли уже добавленного участника. */
public record ProjectMemberRoleRequest(
    @NotNull(message = "roleId is required")
    Integer roleId
) {}
