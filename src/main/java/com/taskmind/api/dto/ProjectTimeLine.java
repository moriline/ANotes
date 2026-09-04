package com.taskmind.api.dto;

/** Строка отчёта по времени: сколько пользователь списал на один проект за период. */
public record ProjectTimeLine(
    Integer projectId,
    String projectName,
    long totalSeconds,
    double totalHours,
    int entryCount
) {}
