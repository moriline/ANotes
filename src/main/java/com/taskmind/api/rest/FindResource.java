package com.taskmind.api.rest;

import com.taskmind.api.dto.FindTasksRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.spi.SortDirection;
import com.taskmind.domain.spi.TaskSearchCriteria;
import com.taskmind.domain.spi.TaskSortField;
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

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    @Inject AuthService authService;
    @Inject ProjectAccessService projectAccessService;
    @Inject TaskService taskService;

    @POST
    public ResponseWrapper find(FindTasksRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());

        int limit = PagingParams.limit(req.limit, DEFAULT_LIMIT, MAX_LIMIT);
        int offset = PagingParams.offset(req.offset);

        Set<Integer> accessibleProjectIds = projectAccessService.accessibleProjectIds(userId);
        if (accessibleProjectIds.isEmpty()) {
            return ResponseWrapper.empty(limit, offset);
        }

        Collection<Integer> scope;
        if (req.projectId != null) {
            if (!accessibleProjectIds.contains(req.projectId)) {
                return ResponseWrapper.empty(limit, offset);
            }
            scope = List.of(req.projectId);
        } else {
            scope = accessibleProjectIds;
        }

        var criteria = new TaskSearchCriteria(
            scope,
            req.titleSearch,
            req.contentSearch,
            assignee(req, userId),
            req.statusId,
            req.isArchived,
            sortField(req.sortBy),
            sortDirection(req.sortDir),
            limit,
            offset
        );

        List<TaskResponse> responses = taskService.search(criteria).stream()
            .map(TaskResponse::from)
            .toList();

        return new ResponseWrapper(responses, taskService.countMatching(criteria), limit, offset);
    }

    /**
     * Противоречивый запрос («мои задачи», но исполнитель — кто-то другой) лучше
     * отклонить, чем молча выбрать одно из двух.
     */
    private static Integer assignee(FindTasksRequest req, Integer callerId) {
        if (!Boolean.TRUE.equals(req.assignedToMe)) {
            return req.assignedUserId;
        }
        if (req.assignedUserId != null && !req.assignedUserId.equals(callerId)) {
            throw new BadRequestException(
                "assignedToMe и assignedUserId противоречат друг другу");
        }
        return callerId;
    }

    private static TaskSortField sortField(String requested) {
        if (requested == null || requested.isBlank()) {
            return TaskSortField.UPDATED_AT;
        }
        try {
            return TaskSortField.fromValue(requested);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Сортировать можно по createdAt, updatedAt, dueDate или title");
        }
    }

    private static SortDirection sortDirection(String requested) {
        if (requested == null || requested.isBlank()) {
            return SortDirection.DESC;
        }
        try {
            return SortDirection.fromValue(requested);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Направление сортировки — asc или desc");
        }
    }

    /**
     * {@code total} — сколько задач подходит под условия целиком, без учёта
     * страницы: без него по выдаче нельзя понять, есть ли ещё что-то дальше.
     */
    public static record ResponseWrapper(List<TaskResponse> tasks, long total, int limit, int offset) {
        static ResponseWrapper empty(int limit, int offset) {
            return new ResponseWrapper(List.of(), 0, limit, offset);
        }
    }
}
