package com.taskmind.api.rest;

import com.taskmind.api.dto.TimeEntryRequest;
import com.taskmind.application.service.PermissionService;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.Action;
import com.taskmind.domain.model.Task;
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
    @Inject PermissionService permissionService;

    /**
     * Списывает время от имени вызывающего. Автор определяется по токену, а не по
     * телу запроса. Списывать время может тот, кто работает над задачей, — право
     * {@code task:update} (не Guest и не Client); смотреть суммарное время может
     * любой участник проекта. Раньше и то и другое было доступно любому
     * залогиненному пользователю.
     */
    @POST
    public TimeEntry logTime(@PathParam("taskId") Integer taskId, @Valid TimeEntryRequest req, @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);
        Integer callerId = guard.callerId(sec);
        if (!permissionService.canPerform(callerId, task.projectId(), Action.TASK_UPDATE)) {
            throw new ForbiddenException("Нужно право task:update, чтобы списывать время на задачу");
        }
        return taskService.logTime(taskId, callerId, req.seconds(), req.description(), req.startTime());
    }

    @GET
    public Long getTotalTime(@PathParam("taskId") Integer taskId, @Context SecurityContext sec) {
        guard.requireAccessibleTask(taskId, sec);
        Long total = taskService.getTotalTimeForTask(taskId);
        return total != null ? total : 0L;
    }
}
