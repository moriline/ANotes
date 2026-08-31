package com.taskmind.api.rest;

import com.taskmind.application.service.AuthService;
import com.taskmind.application.service.ProjectAccessService;
import com.taskmind.application.service.TaskService;
import com.taskmind.domain.model.Task;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Общая проверка для ресурсов, адресующих задачу напрямую по taskId, без
 * projectId в пути: задача должна существовать, а вызывающий — иметь доступ к её
 * проекту.
 */
@ApplicationScoped
public class TaskAccessGuard {

    @Inject AuthService authService;
    @Inject TaskService taskService;
    @Inject ProjectAccessService projectAccessService;

    public Integer callerId(SecurityContext sec) {
        return authService.getUserIdFromToken(sec.getUserPrincipal().getName());
    }

    public Task requireAccessibleTask(Integer taskId, SecurityContext sec) {
        Task task = taskService.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Задача " + taskId + " не найдена"));

        if (!projectAccessService.canAccess(callerId(sec), task.projectId())) {
            throw new ForbiddenException("Нет доступа к проекту " + task.projectId());
        }
        return task;
    }
}
