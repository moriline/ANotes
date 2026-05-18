package com.taskmind.api.rest;

import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.TimeEntry;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api/tasks/{taskId}/time")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TimeEntryResource {

    @Inject TaskService taskService;

    @POST
    public TimeEntry logTime(@PathParam("taskId") Integer taskId, Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        long seconds = Long.parseLong(body.get("seconds").toString());
        String description = (String) body.get("description");
        return taskService.logTime(taskId, userId, seconds, description);
    }

    @GET
    public Long getTotalTime(@PathParam("taskId") Integer taskId) {
        Long total = taskService.getTotalTimeForTask(taskId);
        return total != null ? total : 0L;
    }
}
