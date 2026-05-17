package com.taskmind.api.rest;

import com.taskmind.api.dto.ProjectStatusRequest;
import com.taskmind.api.dto.ProjectStatusResponse;
import com.taskmind.infrastructure.db.ProjectStatusEntity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
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

    @POST
    @Transactional
    public Response create(@Valid ProjectStatusRequest req) {
        var entity = new ProjectStatusEntity();
        entity.projectId = req.projectId();
        entity.statusName = req.statusName();
        entity.statusColor = req.statusColor();
        entity.statusOrder = req.statusOrder();
        entity.isDefault = req.isDefault();
        entity.isClosed = req.isClosed();
        entity.persist();
        return Response.status(Response.Status.CREATED).entity(ProjectStatusResponse.fromEntity(entity)).build();
    }

    @GET
    @Path("/project/{projectId}")
    public List<ProjectStatusResponse> listByProject(@PathParam("projectId") Integer projectId) {
        return ProjectStatusEntity.<ProjectStatusEntity>list("projectId", projectId).stream()
            .map(ProjectStatusResponse::fromEntity)
            .toList();
    }
}
