package com.taskmind.api.rest;

import com.taskmind.api.dto.TimeEntryRequest;
import com.taskmind.application.service.AuthService;
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
    @Inject AuthService authService;

    /**
     * Списывает время от имени вызывающего. Раньше {@code userId} приходил в теле
     * запроса, то есть любой залогиненный пользователь мог записать часы на чужой
     * счёт; теперь автор определяется исключительно по токену.
     */
    @POST
    public TimeEntry logTime(@PathParam("taskId") Integer taskId, @Valid TimeEntryRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        return taskService.logTime(taskId, userId, req.seconds(), req.description());
    }

    @GET
    public Long getTotalTime(@PathParam("taskId") Integer taskId) {
        Long total = taskService.getTotalTimeForTask(taskId);
        return total != null ? total : 0L;
    }
}
