package com.taskmind.api.rest;

import com.taskmind.api.dto.ActivityResponse;
import com.taskmind.application.service.ActivityLogService;
import com.taskmind.application.service.PermissionService;
import com.taskmind.domain.model.Task;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

/** История одной задачи: назначения, переводы по статусам, записанные выводы. */
@Path("/api/tasks/{taskId}/activity")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskActivityResource {

    @Inject TaskAccessGuard guard;
    @Inject ActivityLogService activityLog;
    @Inject PermissionService permissionService;

    @GET
    public List<ActivityResponse> list(@PathParam("taskId") Integer taskId,
                                       @QueryParam("limit") Integer limit,
                                       @QueryParam("offset") Integer offset,
                                       @Context SecurityContext sec) {
        Task task = guard.requireAccessibleTask(taskId, sec);
        boolean includeInternal = permissionService.seesInternalContent(guard.callerId(sec), task.projectId());

        return activityLog.listByTask(
            taskId,
            PagingParams.limit(limit, ActivityLogService.DEFAULT_LIMIT, ActivityLogService.MAX_LIMIT),
            PagingParams.offset(offset),
            includeInternal);
    }
}
