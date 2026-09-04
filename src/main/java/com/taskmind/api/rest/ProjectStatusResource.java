package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectStatusRequest;
import com.taskmind.api.dto.ProjectStatusResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.ProjectService;
import com.taskmind.application.service.ProjectStatusService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/project-statuses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectStatusResource {

    @Inject ProjectStatusService service;
    @Inject AuthService authService;
    @Inject ProjectService projectService;
    @Inject ProjectAccessService projectAccessService;

    /**
     * Заводить статусы можно только в проекте, к которому у вызывающего есть
     * доступ. Раньше проверки не было: любой залогиненный добавлял колонки на
     * чужую доску, зная лишь projectId.
     */
    @POST
    public Response create(@Valid ProjectStatusRequest req, @Context SecurityContext sec) {
        if (req.projectId() == null) {
            throw new BadRequestException("projectId обязателен");
        }
        requireProjectAccess(req.projectId(), sec);
        var entity = service.create(req.projectId(), req.statusName(), req.statusColor(),
            req.statusOrder(), req.isDefault(), req.isClosed());
        return Response.status(Response.Status.CREATED).entity(ProjectStatusResponse.fromEntity(entity)).build();
    }

    @GET
    @Path("/project/{projectId}")
    public List<ProjectStatusResponse> listByProject(@PathParam("projectId") Integer projectId,
                                                     @Context SecurityContext sec) {
        requireProjectAccess(projectId, sec);
        return service.listByProject(projectId).stream()
            .map(ProjectStatusResponse::fromEntity)
            .toList();
    }

    private void requireProjectAccess(Integer projectId, SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        projectService.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Проект " + projectId + " не найден"));
        if (!projectAccessService.canAccess(userId, projectId)) {
            throw new ForbiddenException("Нет доступа к проекту " + projectId);
        }
    }
}
