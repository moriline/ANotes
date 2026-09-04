package com.taskmind.api.rest;

import com.taskmind.api.dto.TimeReportResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.TimeReportService;
import com.taskmind.application.service.UserService;
import com.taskmind.domain.model.User;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

/**
 * Отчёт по списанному времени: {@code GET /api/reports/time?userId=&year=&month=}.
 *
 * <ul>
 *   <li>{@code userId} не задан — отчёт по себе;</li>
 *   <li>{@code year} не задан — текущий год (UTC);</li>
 *   <li>{@code month} (1–12) не задан — весь год.</li>
 * </ul>
 *
 * <p>Кто чей отчёт видит: свой — всегда и целиком; глобальный админ — любой и
 * целиком; остальные — только по пользователю, с которым есть общий проект, и
 * только время на этих общих проектах (чужие проекты в разбивку не попадают).
 */
@Path("/api/reports/time")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TimeReportResource {

    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 9999;

    @Inject AuthService authService;
    @Inject UserService userService;
    @Inject ProjectAccessService projectAccess;
    @Inject TimeReportService reportService;

    @GET
    public TimeReportResponse report(@QueryParam("userId") Integer userIdParam,
                                     @QueryParam("year") Integer yearParam,
                                     @QueryParam("month") Integer monthParam,
                                     @Context SecurityContext sec) {
        Integer callerId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        Integer targetId = userIdParam != null ? userIdParam : callerId;

        User target = userService.findById(targetId)
            .orElseThrow(() -> new NotFoundException("Пользователь " + targetId + " не найден"));

        Set<Integer> projectScope = resolveScope(callerId, targetId, sec);
        int year = resolveYear(yearParam);
        Integer month = validateMonth(monthParam);

        return reportService.build(target, year, month, projectScope);
    }

    /** {@code null} — без ограничения по проектам (свой отчёт или админ). */
    private Set<Integer> resolveScope(Integer callerId, Integer targetId, SecurityContext sec) {
        if (targetId.equals(callerId) || sec.isUserInRole("ADMIN")) {
            return null;
        }
        Set<Integer> shared = new HashSet<>(projectAccess.accessibleProjectIds(callerId));
        shared.retainAll(projectAccess.accessibleProjectIds(targetId));
        if (shared.isEmpty()) {
            throw new ForbiddenException(
                "Отчёт по пользователю " + targetId + " виден только тем, кто с ним в общем проекте");
        }
        return shared;
    }

    private int resolveYear(Integer year) {
        if (year == null) {
            return Year.now(ZoneOffset.UTC).getValue();
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BadRequestException("year должен быть в диапазоне " + MIN_YEAR + "–" + MAX_YEAR);
        }
        return year;
    }

    private Integer validateMonth(Integer month) {
        if (month == null) {
            return null;
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException("month должен быть от 1 до 12");
        }
        return month;
    }
}
