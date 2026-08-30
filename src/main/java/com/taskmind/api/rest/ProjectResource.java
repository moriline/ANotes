package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.ProjectResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectService;
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

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        service.deleteProject(id);
        return Response.noContent().build();
    }
}
