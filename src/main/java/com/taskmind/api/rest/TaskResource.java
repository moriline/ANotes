package com.taskmind.api.rest;

import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.TaskService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/projects/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskResource {

    @Inject TaskService service;
    @Inject AuthService authService;

    @POST
    public Response create(@PathParam("projectId") Integer projectId, @Valid TaskRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        var created = service.createTask(projectId, req.title(), req.description(), req.tags(), userId);
        return Response.status(Response.Status.CREATED).entity(TaskResponse.from(created)).build();
    }

    @GET
    public List<TaskResponse> list(@PathParam("projectId") Integer projectId) {
        return service.listByProject(projectId).stream().map(TaskResponse::from).toList();
    }

    @DELETE
    @Path("/{taskId}")
    public Response delete(@PathParam("taskId") Integer taskId) {
        service.deleteTask(taskId);
        return Response.noContent().build();
    }
}
