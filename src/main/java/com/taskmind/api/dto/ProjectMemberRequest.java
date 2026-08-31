package com.taskmind.api.dto;

import jakarta.validation.constraints.NotNull;

/** Добавление участника в проект. roleId — из справочника projectRoles (1=Admin … 5=Disabled). */
public record ProjectMemberRequest(
    @NotNull(message = "userId is required")
    Integer userId,

    @NotNull(message = "roleId is required")
    Integer roleId
) {}
