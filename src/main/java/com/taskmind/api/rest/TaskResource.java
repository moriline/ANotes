package com.taskmind.api.rest;

import com.taskmind.api.dto.TaskRequest;
import com.taskmind.api.dto.TaskResponse;
import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.ProjectStatusService;
import com.taskmind.application.service.TaskService;
import com.taskmind.application.service.UserService;
import com.taskmind.domain.model.Task;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/projects/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class TaskResource {

    @Inject TaskService service;
    @Inject AuthService authService;
    @Inject ProjectAccessService projectAccessService;
    @Inject ProjectStatusService projectStatusService;
    @Inject UserService userService;

    @POST
    public Response create(@PathParam("projectId") Integer projectId, @Valid TaskRequest req, @Context SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        var created = service.createTask(projectId, req.title(), req.description(), req.tags(), userId);
        return Response.status(Response.Status.CREATED).entity(TaskResponse.from(created)).build();
    }

    @GET
    public List<TaskResponse> list(@PathParam("projectId") Integer projectId, @Context SecurityContext sec) {
        requireProjectAccess(projectId, sec);
        return service.listByProject(projectId).stream().map(TaskResponse::from).toList();
    }

    /**
     * Карточка одной задачи. Раньше задачу нельзя было прочитать по её id: только
     * выгрузить весь список проекта или найти через /api/find.
     */
    @GET
    @Path("/{taskId}")
    public TaskResponse get(@PathParam("projectId") Integer projectId,
                            @PathParam("taskId") Integer taskId,
                            @Context SecurityContext sec) {
        requireProjectAccess(projectId, sec);
        return TaskResponse.from(requireTaskOfProject(projectId, taskId));
    }

    /**
     * Частичное обновление задачи: назначение исполнителя, перевод по статусам,
     * сроки, оценка, архивация, правка текста. До этого задачу нельзя было ни
     * назначить, ни сдвинуть по доске — {@code assignedUserId} и {@code statusId}
     * существовали только в базе.
     *
     * <p>Поля со значением {@code null} остаются без изменений (см. TaskUpdateRequest).
     */
    @PATCH
    @Path("/{taskId}")
    public TaskResponse update(@PathParam("projectId") Integer projectId,
                               @PathParam("taskId") Integer taskId,
                               TaskUpdateRequest req,
                               @Context SecurityContext sec) {
        Integer userId = requireProjectAccess(projectId, sec);
        Task task = requireTaskOfProject(projectId, taskId);

        validateAssignee(projectId, req.assignedUserId());
        validateStatus(projectId, req.statusId());

        return TaskResponse.from(service.applyUpdate(task, req, userId));
    }

    @DELETE
    @Path("/{taskId}")
    public Response delete(@PathParam("taskId") Integer taskId) {
        service.deleteTask(taskId);
        return Response.noContent().build();
    }

    /** Проверяет доступ и заодно возвращает id вызывающего — он нужен для ленты активности. */
    private Integer requireProjectAccess(Integer projectId, SecurityContext sec) {
        Integer userId = authService.getUserIdFromToken(sec.getUserPrincipal().getName());
        if (!projectAccessService.canAccess(userId, projectId)) {
            throw new ForbiddenException("Нет доступа к проекту " + projectId);
        }
        return userId;
    }

    private Task requireTaskOfProject(Integer projectId, Integer taskId) {
        return service.findById(taskId)
            .filter(task -> task.projectId().equals(projectId))
            .orElseThrow(() -> new NotFoundException("Задача " + taskId + " не найдена в проекте " + projectId));
    }

    /** Исполнителем можно поставить только того, кто сам видит этот проект. */
    private void validateAssignee(Integer projectId, Integer assignedUserId) {
        if (assignedUserId == null) {
            return;
        }
        if (userService.findById(assignedUserId).isEmpty()) {
            throw new BadRequestException("Пользователь " + assignedUserId + " не существует");
        }
        if (!projectAccessService.canAccess(assignedUserId, projectId)) {
            throw new BadRequestException(
                "Пользователь " + assignedUserId + " не участник проекта " + projectId);
        }
    }

    /** Статус обязан принадлежать этому же проекту: внешний ключ такого не проверяет. */
    private void validateStatus(Integer projectId, Integer statusId) {
        if (statusId == null) {
            return;
        }
        boolean belongsToProject = projectStatusService.listByProject(projectId).stream()
            .anyMatch(status -> status.id.equals(statusId));
        if (!belongsToProject) {
            throw new BadRequestException(
                "Статус " + statusId + " не принадлежит проекту " + projectId);
        }
    }
}
