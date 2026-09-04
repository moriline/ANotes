package com.taskmind.api.dto;

import java.util.List;

/**
 * Отчёт по списанному времени одного пользователя за месяц или год.
 *
 * @param month {@code null}, если запрошен весь год
 * @param from  начало окна, epoch-ms, включительно
 * @param to    конец окна, epoch-ms, не включая
 * @param byProject разбивка по проектам, по убыванию времени; для коллеги (не
 *        сам пользователь и не админ) — только по общим с ним проектам
 */
public record TimeReportResponse(
    Integer userId,
    String username,
    int year,
    Integer month,
    long from,
    long to,
    long totalSeconds,
    double totalHours,
    int entryCount,
    List<ProjectTimeLine> byProject
) {}
