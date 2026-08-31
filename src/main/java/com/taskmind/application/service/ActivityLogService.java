package com.taskmind.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.api.dto.ActivityResponse;
import com.taskmind.domain.model.ActivityAction;
import com.taskmind.infrastructure.db.ActivityLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Лента «кто что сделал». Таблица {@code activityLog} была в схеме с самого
 * начала, но её никто не заполнял и не читал — единственными записями были четыре
 * строки из сида.
 *
 * <p>Запись события никогда не должна ронять основную операцию: если лог почему-то
 * не пишется, задача всё равно должна создаться. Поэтому ошибки здесь гасятся.
 */
@ApplicationScoped
public class ActivityLogService {

    /** actionDetails в схеме — VARCHAR(2000). */
    private static final int MAX_DETAILS_LENGTH = 2000;

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject UserService userService;

    @Transactional
    public void record(Integer projectId, Integer taskId, Integer userId,
                       ActivityAction action, Map<String, Object> details) {
        try {
            var entity = new ActivityLogEntity();
            entity.projectId = projectId;
            entity.taskId = taskId;
            entity.userId = userId;
            entity.actionType = action.name();
            entity.actionDetails = serialize(details);
            entity.createdAt = Instant.now().toEpochMilli();
            entity.persist();
        } catch (Exception e) {
            // Лента — вспомогательная вещь: молча пропускаем запись, но не срываем операцию.
            System.err.println("Не удалось записать событие " + action + ": " + e.getMessage());
        }
    }

    public List<ActivityResponse> listByProject(Integer projectId, int limit, int offset) {
        return toResponses(ActivityLogEntity.<ActivityLogEntity>find(
                "projectId = ?1 order by createdAt desc, id desc", projectId)
            .range(offset, offset + limit - 1)
            .list());
    }

    public List<ActivityResponse> listByTask(Integer taskId, int limit, int offset) {
        return toResponses(ActivityLogEntity.<ActivityLogEntity>find(
                "taskId = ?1 order by createdAt desc, id desc", taskId)
            .range(offset, offset + limit - 1)
            .list());
    }

    private List<ActivityResponse> toResponses(List<ActivityLogEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    private ActivityResponse toResponse(ActivityLogEntity entity) {
        String username = entity.userId == null ? null
            : userService.findById(entity.userId).map(user -> user.username()).orElse(null);

        return new ActivityResponse(
            entity.id,
            entity.projectId,
            entity.taskId,
            entity.userId,
            username,
            entity.actionType,
            deserialize(entity.actionDetails),
            entity.createdAt != null ? Instant.ofEpochMilli(entity.createdAt) : null
        );
    }

    private String serialize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            String json = MAPPER.writeValueAsString(details);
            return json.length() > MAX_DETAILS_LENGTH ? json.substring(0, MAX_DETAILS_LENGTH) : json;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            // Строки из сида и обрезанный JSON не должны ломать чтение ленты.
            return null;
        }
    }
}
