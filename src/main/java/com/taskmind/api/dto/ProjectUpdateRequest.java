package com.taskmind.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Частичное обновление проекта: {@code null} в поле означает «не трогать».
 * Как и у {@link TaskUpdateRequest}, снять уже проставленное значение через PATCH
 * нельзя (присланный null неотличим от отсутствующего поля); для тэгов пустой
 * список {@code []} очищает их полностью.
 */
public record ProjectUpdateRequest(
    @Size(min = 3, max = 50, message = "Name must be 3-50 chars")
    String name,

    String description,

    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "color must be a hex like #4A90D9")
    String color,

    @Size(max = 50, message = "icon name is too long")
    String icon,

    @Size(max = 30, message = "не больше 30 тэгов на проект")
    List<String> tags
) {}
