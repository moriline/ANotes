package com.taskmind.api.rest;

import com.taskmind.api.dto.TimeEntryRequest;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.TimeEntry;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/tasks/{taskId}/time")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TimeEntryResource {

    @Inject TaskService taskService;
    @Inject TaskAccessGuard guard;

    /**
     * Списывает время от имени вызывающего. Автор определяется по токену, а не по
     * телу запроса; списать и посмотреть время можно только по задаче проекта, к
     * которому у вызывающего есть доступ (раньше проверки не было — любой
     * залогиненный писал часы на любую задачу).
     */
    @POST
    public TimeEntry logTime(@PathParam("taskId") Integer taskId, @Valid TimeEntryRequest req, @Context SecurityContext sec) {
        guard.requireAccessibleTask(taskId, sec);
        return taskService.logTime(taskId, guard.callerId(sec), req.seconds(), req.description(), req.startTime());
    }

    @GET
    public Long getTotalTime(@PathParam("taskId") Integer taskId, @Context SecurityContext sec) {
        guard.requireAccessibleTask(taskId, sec);
        Long total = taskService.getTotalTimeForTask(taskId);
        return total != null ? total : 0L;
    }
}
