package com.taskmind.domain.model;

import java.util.Set;

public record ProjectRole(
    Integer id,
    String name,
    String description,
    Set<Action> permissions
) {}
