package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectStatusRequest;
import com.taskmind.api.dto.ProjectStatusResponse;
import com.taskmind.application.service.ProjectStatusService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/project-statuses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ProjectStatusResource {

    @Inject ProjectStatusService service;

    @POST
    public Response create(@Valid ProjectStatusRequest req) {
        var entity = service.create(req.projectId(), req.statusName(), req.statusColor(),
            req.statusOrder(), req.isDefault(), req.isClosed());
        return Response.status(Response.Status.CREATED).entity(ProjectStatusResponse.fromEntity(entity)).build();
    }

    @GET
    @Path("/project/{projectId}")
    public List<ProjectStatusResponse> listByProject(@PathParam("projectId") Integer projectId) {
        return service.listByProject(projectId).stream()
            .map(ProjectStatusResponse::fromEntity)
            .toList();
    }
}
