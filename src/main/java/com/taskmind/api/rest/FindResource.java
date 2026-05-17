package com.taskmind.api.rest;

import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.infrastructure.db.TaskEntity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/find")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FindResource {

    @POST
    public ResponseWrapper find(FindTasksRequest req) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (req.projectId != null) {
            query.append(" AND projectId = :projectId");
            params.put("projectId", req.projectId);
        }
        if (req.titleSearch != null && !req.titleSearch.isBlank()) {
            query.append(" AND title LIKE :title");
            params.put("title", "%" + req.titleSearch + "%");
        }
        if (req.assignedUserId != null) {
            query.append(" AND assignedUserId = :assignedUserId");
            params.put("assignedUserId", req.assignedUserId);
        }
        if (req.statusId != null) {
            query.append(" AND statusId = :statusId");
            params.put("statusId", req.statusId);
        }
        if (req.isArchived != null) {
            query.append(" AND isArchived = :isArchived");
            params.put("isArchived", req.isArchived);
        }

        List<TaskEntity> tasks = TaskEntity.list(query.toString(), params);
        List<TaskResponse> responses = tasks.stream()
            .map(TaskEntity::toDomainModel)
            .map(TaskResponse::from)
            .collect(Collectors.toList());

        return new ResponseWrapper(responses);
    }

    public static record ResponseWrapper(List<TaskResponse> tasks) {}
}
