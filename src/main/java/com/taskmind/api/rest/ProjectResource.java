package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectRequest;
import com.taskmind.api.dto.ProjectResponse;
import com.taskmind.application.service.ProjectService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject ProjectService service;

    @POST
    public Response create(@Valid ProjectRequest request) {
        var created = service.createProject(request.name());
        return Response.status(Response.Status.CREATED).entity(ProjectResponse.fromDomain(created)).build();
    }

    @GET
    public List<ProjectResponse> list() {
        return service.listActiveProjects().stream()
            .map(ProjectResponse::fromDomain)
            .toList();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        service.deleteProject(id);
        return Response.noContent().build();
    }
}
