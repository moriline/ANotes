package com.taskmind.application.service;

import com.taskmind.api.dto.CommentResponse;
import com.taskmind.domain.model.ActivityAction;
import com.taskmind.domain.model.Task;
import com.taskmind.domain.model.Visibility;
import com.taskmind.domain.spi.TaskRepository;
import com.taskmind.infrastructure.db.CommentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommentService {

    /** Сколько символов тела кладём в ленту, чтобы событие читалось без второго запроса. */
    private static final int EXCERPT_LENGTH = 80;

    @Inject TaskRepository taskRepository;
    @Inject ProjectAccessService projectAccess;
    @Inject PermissionService permissionService;
    @Inject ActivityLogService activityLog;

    @Transactional
    public CommentResponse addComment(Integer taskId, Integer userId, String content, Visibility visibility) {
        Task task = requireAccessibleTask(taskId, userId);
        Visibility vis = effectiveVisibility(userId, task.projectId(), visibility);

        var entity = new CommentEntity();
        entity.taskId = taskId;
        entity.userId = userId;
        entity.content = content;
        entity.visibility = vis.name();
        entity.persist();

        activityLog.record(task.projectId(), taskId, userId,
            ActivityAction.COMMENT_ADDED, details(entity.id, content), vis);
        return mapToResponse(entity);
    }

    /**
     * @param includeInternal видит ли вызывающий внутренние комментарии;
     *        {@code false} для заказчика — он получает только {@code PUBLIC}.
     */
    public List<CommentResponse> getCommentsByTask(Integer taskId, boolean includeInternal) {
        var comments = includeInternal
            ? CommentEntity.<CommentEntity>list("taskId", taskId)
            : CommentEntity.<CommentEntity>list("taskId = ?1 and visibility = ?2", taskId, Visibility.PUBLIC.name());
        return comments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Правит текст, и — если {@code visibility} задан — заодно видимость. {@code null}
     * оставляет прежнюю: смена текста не должна молча делать внутреннюю заметку публичной.
     */
    @Transactional
    public CommentResponse updateComment(Integer commentId, Integer userId, String content, Visibility visibility) {
        CommentEntity entity = CommentEntity.findById(commentId);
        if (entity == null) throw new ResourceNotFoundException("Комментарий " + commentId + " не найден");
        Task task = requireAccessibleTask(entity.taskId, userId);
        if (!entity.userId.equals(userId)) throw new AccessDeniedException("Редактировать можно только свой комментарий");

        entity.content = content;
        if (visibility != null) {
            entity.visibility = effectiveVisibility(userId, task.projectId(), visibility).name();
        }
        entity.isEdited = true;
        entity.updatedAt = Instant.now().toEpochMilli();

        activityLog.record(task.projectId(), entity.taskId, userId,
            ActivityAction.COMMENT_EDITED, details(entity.id, content), Visibility.fromString(entity.visibility));
        return mapToResponse(entity);
    }

    @Transactional
    public void deleteComment(Integer commentId, Integer userId) {
        CommentEntity entity = CommentEntity.findById(commentId);
        // Молчаливый выход отвечал 204, то есть «удалил» несуществующее.
        if (entity == null) throw new ResourceNotFoundException("Комментарий " + commentId + " не найден");
        Task task = requireAccessibleTask(entity.taskId, userId);
        if (!entity.userId.equals(userId)) throw new AccessDeniedException("Удалить можно только свой комментарий");

        // Событие пишем до физического удаления; тело в ленту не переносим — удалённый
        // текст не должен всплывать в истории (в т. ч. по требованиям об удалении данных).
        var deleted = new HashMap<String, Object>();
        deleted.put("commentId", entity.id);
        activityLog.record(task.projectId(), entity.taskId, userId,
            ActivityAction.COMMENT_DELETED, deleted, Visibility.fromString(entity.visibility));

        entity.delete();
    }

    /**
     * Задача существует (иначе 404) и вызывающий — участник её проекта (иначе 403).
     * Создатель проекта попадает в участники автоматически, так что для своих
     * задач эта проверка всегда проходит. Ту же проверку делает TaskAccessGuard в
     * api.rest, но {@code PUT/DELETE /api/comments/{id}} приходит без taskId в
     * пути, и ресурс проверить проект сам не может — поэтому она продублирована
     * здесь, на уровне сервиса.
     */
    private Task requireAccessibleTask(Integer taskId, Integer userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Задача " + taskId + " не найдена"));
        if (!projectAccess.canAccess(userId, task.projectId())) {
            throw new AccessDeniedException("Нет доступа к проекту " + task.projectId());
        }
        return task;
    }

    /**
     * Заказчик не создаёт внутренние заметки: любой запрошенный им {@code INTERNAL}/
     * {@code SYSTEM} схлопывается в {@code PUBLIC}. Отсутствие значения — тоже {@code PUBLIC}.
     */
    private Visibility effectiveVisibility(Integer userId, Integer projectId, Visibility requested) {
        Visibility vis = requested != null ? requested : Visibility.PUBLIC;
        if (vis != Visibility.PUBLIC && !permissionService.seesInternalContent(userId, projectId)) {
            return Visibility.PUBLIC;
        }
        return vis;
    }

    private static Map<String, Object> details(Integer commentId, String content) {
        var details = new HashMap<String, Object>();
        details.put("commentId", commentId);
        details.put("excerpt", excerpt(content));
        return details;
    }

    private static String excerpt(String content) {
        if (content == null) return null;
        String trimmed = content.strip();
        return trimmed.length() <= EXCERPT_LENGTH ? trimmed : trimmed.substring(0, EXCERPT_LENGTH) + "…";
    }

    private CommentResponse mapToResponse(CommentEntity e) {
        return new CommentResponse(
            e.id,
            e.taskId,
            e.userId,
            e.content,
            Visibility.fromString(e.visibility),
            e.createdAt != null ? Instant.ofEpochMilli(e.createdAt) : null,
            e.isEdited,
            e.updatedAt != null ? Instant.ofEpochMilli(e.updatedAt) : null
        );
    }
}
