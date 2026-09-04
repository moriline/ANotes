package com.taskmind.application.service;

import com.taskmind.api.dto.CommentResponse;
import com.taskmind.domain.model.Visibility;
import com.taskmind.infrastructure.db.CommentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommentService {

    @Transactional
    public CommentResponse addComment(Integer taskId, Integer userId, String content, Visibility visibility) {
        var entity = new CommentEntity();
        entity.taskId = taskId;
        entity.userId = userId;
        entity.content = content;
        entity.visibility = (visibility != null ? visibility : Visibility.PUBLIC).name();
        entity.persist();
        return mapToResponse(entity);
    }

    public List<CommentResponse> getCommentsByTask(Integer taskId) {
        return CommentEntity.<CommentEntity>list("taskId", taskId).stream()
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
        if (!entity.userId.equals(userId)) throw new AccessDeniedException("Редактировать можно только свой комментарий");

        entity.content = content;
        if (visibility != null) {
            entity.visibility = visibility.name();
        }
        entity.isEdited = true;
        entity.updatedAt = Instant.now().toEpochMilli();
        return mapToResponse(entity);
    }

    @Transactional
    public void deleteComment(Integer commentId, Integer userId) {
        CommentEntity entity = CommentEntity.findById(commentId);
        // Молчаливый выход отвечал 204, то есть «удалил» несуществующее.
        if (entity == null) throw new ResourceNotFoundException("Комментарий " + commentId + " не найден");
        if (!entity.userId.equals(userId)) throw new AccessDeniedException("Удалить можно только свой комментарий");
        entity.delete();
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
