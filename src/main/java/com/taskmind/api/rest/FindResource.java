package com.taskmind.api.rest;

import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.spi.TaskSearchCriteria;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Task search restricted to the projects the caller can actually see:
 * projects they own plus projects they are a member of. A search without a
 * {@code projectId} spans exactly that set; a search with a {@code projectId}
 * outside that set yields an empty result instead of leaking foreign tasks.
 *
 * <p>{@code contentSearch} ищет по всему тексту задачи, включая summary и
 * обсуждение — через него модель находит свои прежние выводы.
 */
@Path("/api/find")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FindResource {

    @Inject AuthService authService;
    @Inject ProjectAccessService projectAccessService;
    @Inject TaskService taskService;

    @POST
    public ResponseWrapper find(FindTasksRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        Set<Integer> accessibleProjectIds = projectAccessService.accessibleProjectIds(userId);
        if (accessibleProjectIds.isEmpty()) {
            return new ResponseWrapper(List.of());
        }

        Collection<Integer> scope;
        if (req.projectId != null) {
            if (!accessibleProjectIds.contains(req.projectId)) {
                return new ResponseWrapper(List.of());
            }
            scope = List.of(req.projectId);
        } else {
            scope = accessibleProjectIds;
        }

        var criteria = new TaskSearchCriteria(
            scope,
            req.titleSearch,
            req.contentSearch,
            req.assignedUserId,
            req.statusId,
            req.isArchived
        );

        List<TaskResponse> responses = taskService.search(criteria).stream()
            .map(TaskResponse::from)
            .toList();

        return new ResponseWrapper(responses);
    }

    public static record ResponseWrapper(List<TaskResponse> tasks) {}
}
