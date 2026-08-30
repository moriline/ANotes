package com.taskmind.api.rest;

import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.domain.spi.MembershipRepository;
import com.taskmind.infrastructure.db.ProjectEntity;
import com.taskmind.infrastructure.db.TaskEntity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task search restricted to the projects the caller can actually see:
 * projects they own plus projects they are a member of. A search without a
 * {@code projectId} spans exactly that set; a search with a {@code projectId}
 * outside that set yields an empty result instead of leaking foreign tasks.
 */
@Path("/api/find")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FindResource {

    @Inject AuthService authService;
    @Inject MembershipRepository membershipRepository;

    @POST
    public ResponseWrapper find(FindTasksRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        Set<Integer> accessibleProjectIds = accessibleProjectIds(userId);
        if (accessibleProjectIds.isEmpty()) {
            return new ResponseWrapper(List.of());
        }

        List<Integer> scope;
        if (req.projectId != null) {
            if (!accessibleProjectIds.contains(req.projectId)) {
                return new ResponseWrapper(List.of());
            }
            scope = List.of(req.projectId);
        } else {
            scope = new ArrayList<>(accessibleProjectIds);
        }

        StringBuilder query = new StringBuilder("projectId IN :projectIds");
        Map<String, Object> params = new HashMap<>();
        params.put("projectIds", scope);

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

        List<TaskResponse> responses = TaskEntity.<TaskEntity>list(query.toString(), params).stream()
            .map(TaskEntity::toDomainModel)
            .map(TaskResponse::from)
            .collect(Collectors.toList());

        return new ResponseWrapper(responses);
    }

    private Set<Integer> accessibleProjectIds(Integer userId) {
        Set<Integer> ids = new HashSet<>();
        ProjectEntity.<ProjectEntity>list("ownerUserId", userId).forEach(p -> ids.add(p.id));
        membershipRepository.findByUser(userId).forEach(m -> ids.add(m.projectId()));
        return ids;
    }

    public static record ResponseWrapper(List<TaskResponse> tasks) {}
}
