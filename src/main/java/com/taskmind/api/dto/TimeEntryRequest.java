package com.taskmind.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Тело запроса на списание времени. {@code userId} здесь сознательно нет: автор
 * записи берётся из токена, иначе любой пользователь мог бы списать время на
 * чужое имя.
 */
public record TimeEntryRequest(
    @NotNull(message = "seconds is required")
    @Positive(message = "seconds must be positive")
    Long seconds,

    String description
) {}
