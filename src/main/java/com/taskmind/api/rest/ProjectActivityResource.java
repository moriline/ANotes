package com.taskmind.api.rest;

import com.taskmind.api.dto.ActivityResponse;
import com.taskmind.application.service.ActivityLogService;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.ProjectService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * Лента проекта: кто что сделал, свежее сверху. Таблица {@code activityLog} была
 * в схеме с самого начала, но её никто не заполнял и не читал.
 */
@Path("/api/projects/{projectId}/activity")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectActivityResource {

    @Inject AuthService authService;
    @Inject ProjectService projectService;
    @Inject ProjectAccessService projectAccessService;
    @Inject ActivityLogService activityLog;

    @GET
    public List<ActivityResponse> list(@PathParam("projectId") Integer projectId,
                                       @QueryParam("limit") Integer limit,
                                       @QueryParam("offset") Integer offset,
                                       @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        projectService.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Проект " + projectId + " не найден"));

        if (!projectAccessService.canAccess(userId, projectId)) {
            throw new ForbiddenException("Нет доступа к проекту " + projectId);
        }

        return activityLog.listByProject(
            projectId,
            PagingParams.limit(limit, ActivityLogService.DEFAULT_LIMIT, ActivityLogService.MAX_LIMIT),
            PagingParams.offset(offset));
    }
}
