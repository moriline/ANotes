package com.taskmind.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Создание проекта. {@code color}, {@code icon} и {@code tags} необязательны:
 * без цвета проект получает дефолтный ({@link com.taskmind.domain.model.Project#DEFAULT_COLOR}),
 * без тэгов — пустой список. Доступные цвета и иконки отдаёт
 * {@code GET /api/projects/appearance}.
 */
public record ProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 50, message = "Name must be 3-50 chars")
    String name,

    String description,

    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "color must be a hex like #4A90D9")
    String color,

    @Size(max = 50, message = "icon name is too long")
    String icon,

    @Size(max = 30, message = "не больше 30 тэгов на проект")
    List<String> tags
) {
    /** Проект без оформления — только имя и описание. */
    public ProjectRequest(String name, String description) {
        this(name, description, null, null, null);
    }
}
