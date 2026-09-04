package com.taskmind.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Тело запроса на списание времени. {@code userId} здесь сознательно нет: автор
 * записи берётся из токена, иначе любой пользователь мог бы списать время на
 * чужое имя.
 *
 * @param startTime момент, к которому относится работа (epoch-ms). Нужен для
 *        задним числом внесённых записей — «вчера отработал 3 часа». Если не
 *        задан, берётся текущее время. Отчёты за месяц/год фильтруют именно по
 *        нему, а не по времени внесения записи.
 */
public record TimeEntryRequest(
    @NotNull(message = "seconds is required")
    @Positive(message = "seconds must be positive")
    Long seconds,

    String description,

    Long startTime
) {}
