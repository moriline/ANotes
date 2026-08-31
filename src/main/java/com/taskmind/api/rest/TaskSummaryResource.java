package com.taskmind.api.rest;

import com.taskmind.api.dto.TaskSummaryRequest;
import com.taskmind.api.dto.TaskSummaryResponse;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.Task;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Итог по задаче — место, куда модель кладёт вывод, решив задачу. Колонка
 * {@code tasks.summary} существовала с самого начала, но записать в неё через API
 * было нельзя. Найти записанное можно через {@code contentSearch} в /api/find.
 */
@Path("/api/tasks/{taskId}/summary")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskSummaryResource {

    @Inject TaskService taskService;
    @Inject TaskAccessGuard guard;

    @GET
    public TaskSummaryResponse get(@PathParam("taskId") Integer taskId, @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);
        return TaskSummaryResponse.from(task);
    }

    @PUT
    public TaskSummaryResponse put(@PathParam("taskId") Integer taskId,
                                   @Valid TaskSummaryRequest req,
                                   @Context SecurityContext sec) {
        guard.requireAccessibleTask(taskId, sec);
        return TaskSummaryResponse.from(taskService.setSummary(taskId, req.summary(), guard.callerId(sec)));
    }
}
