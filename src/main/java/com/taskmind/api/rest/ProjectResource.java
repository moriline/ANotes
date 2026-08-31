package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.ProjectResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.PermissionService;
import com.taskmind.application.service.ProjectService;
import com.taskmind.domain.model.Action;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectResource {

    @Inject ProjectService service;
    @Inject AuthService authService;
    @Inject ProjectAccessService projectAccessService;
    @Inject PermissionService permissionService;

    @POST
    public Response create(@Valid ProjectRequest request, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        var created = service.createProject(request.name(), request.description(), userId);
        return Response.status(Response.Status.CREATED).entity(ProjectResponse.fromDomain(created)).build();
    }

    @GET
    public List<ProjectResponse> list(@Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        return service.listAccessibleProjects(userId).stream()
            .map(ProjectResponse::fromDomain)
            .toList();
    }

    /** Карточка проекта: без неё открыть проект по ссылке было нельзя, только перебрать список. */
    @GET
    @Path("/{id}")
    public ProjectResponse get(@PathParam("id") Integer id, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        var project = service.findById(id)
            .orElseThrow(() -> new NotFoundException("Проект " + id + " не найден"));

        if (!projectAccessService.canAccess(userId, id)) {
            throw new ForbiddenException("Нет доступа к проекту " + id);
        }
        return ProjectResponse.fromDomain(project);
    }

    /**
     * Удалить проект может владелец или участник с правом {@code project:delete}
     * (по справочнику ролей это только Admin). Раньше проверок не было вообще:
     * любой залогиненный сносил чужой проект, зная его id.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        service.findById(id)
            .orElseThrow(() -> new NotFoundException("Проект " + id + " не найден"));

        if (!permissionService.canPerform(userId, id, Action.PROJECT_DELETE)) {
            throw new ForbiddenException("Нет права удалять проект " + id);
        }

        service.deleteProject(id);
        return Response.noContent().build();
    }
}
