package com.taskmind.api.rest;

import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.application.service.TaskService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/projects/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject TaskService service;

    @POST
    public Response create(@PathParam("projectId") UUID projectId, @Valid TaskRequest req) {
        var created = service.createTask(projectId, req.title(), req.description(), req.tags());
        return Response.status(Response.Status.CREATED).entity(TaskResponse.from(created)).build();
    }

    @GET
    public List<TaskResponse> list(@PathParam("projectId") UUID projectId) {
        return service.listByProject(projectId).stream().map(TaskResponse::from).toList();
    }

    @GET
    @Path("/search")
    public List<TaskResponse> search(@PathParam("projectId") UUID projectId, @QueryParam("q") String query) {
        return service.searchByProject(projectId, query).stream().map(TaskResponse::from).toList();
    }

    @DELETE
    @Path("/{taskId}")
    public Response delete(@PathParam("taskId") UUID taskId) {
        service.deleteTask(taskId);
        return Response.noContent().build();
    }
}
