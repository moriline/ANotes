package com.taskmind.application.service;

import com.taskmind.api.dto.ProjectTimeLine;
import com.taskmind.api.dto.TimeReportResponse;
import com.taskmind.domain.model.User;
import com.taskmind.infrastructure.db.H2TimeEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Считает отчёт по времени. Окно месяца/года берётся в UTC — как и все
 * остальные метки времени в проекте (epoch-ms), чтобы «октябрь» значил одно и то
 * же везде.
 */
@ApplicationScoped
public class TimeReportService {

    @Inject H2TimeEntryRepository timeEntries;

    /**
     * @param month {@code null} — весь год
     * @param projectScope {@code null} — без ограничения по проектам (свой отчёт
     *        или админ); иначе — только эти проекты
     */
    public TimeReportResponse build(User target, int year, Integer month, Set<Integer> projectScope) {
        long from;
        long to;
        if (month != null) {
            YearMonth ym = YearMonth.of(year, month);
            from = startOfDayMillis(ym.atDay(1));
            to = startOfDayMillis(ym.plusMonths(1).atDay(1));
        } else {
            from = startOfDayMillis(LocalDate.of(year, 1, 1));
            to = startOfDayMillis(LocalDate.of(year + 1, 1, 1));
        }

        List<ProjectTimeLine> byProject = timeEntries
            .sumByUserGroupedByProject(target.id(), from, to, projectScope).stream()
            .map(a -> new ProjectTimeLine(
                a.projectId(), a.projectName(), a.totalSeconds(), hours(a.totalSeconds()), a.entryCount()))
            .toList();

        long totalSeconds = byProject.stream().mapToLong(ProjectTimeLine::totalSeconds).sum();
        int entryCount = byProject.stream().mapToInt(ProjectTimeLine::entryCount).sum();

        return new TimeReportResponse(
            target.id(), target.username(), year, month,
            from, to, totalSeconds, hours(totalSeconds), entryCount, byProject);
    }

    private static long startOfDayMillis(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** Часы с двумя знаками после запятой. */
    private static double hours(long seconds) {
        return Math.round(seconds / 3600.0 * 100.0) / 100.0;
    }
}
