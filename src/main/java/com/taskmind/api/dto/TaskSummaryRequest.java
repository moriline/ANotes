package com.taskmind.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Запись итога по задаче. Поле обязательно, чтобы кривое тело запроса не стирало
 * уже записанный вывод молча; чтобы стереть осознанно, надо прислать пустую строку.
 */
public record TaskSummaryRequest(
    @NotNull(message = "summary is required")
    String summary
) {}
