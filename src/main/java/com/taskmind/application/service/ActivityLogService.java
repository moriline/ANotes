package com.taskmind.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.api.dto.ActivityResponse;
import com.taskmind.domain.model.ActivityAction;
import com.taskmind.domain.model.Visibility;
import com.taskmind.infrastructure.db.ActivityLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Лента «кто что сделал». Таблица {@code activityLog} была в схеме с самого
 * начала, но её никто не заполнял и не читал — единственными записями были четыре
 * строки из сида.
 *
 * <p>Событие пишется в той же транзакции, что и сама операция (метод
 * {@link jakarta.transaction.Transactional} с семантикой REQUIRED присоединяется
 * к транзакции вызывающего), и ошибка записи её роняет: лента — это журнал того,
 * что произошло, и «операция прошла, а следа в ленте нет» быть не должно. Раньше
 * ошибки здесь гасились — история молча теряла записи, а сборка об этом не знала.
 */
@ApplicationScoped
public class ActivityLogService {

    /** actionDetails в схеме — VARCHAR(2000). */
    private static final int MAX_DETAILS_LENGTH = 2000;

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Что показываем заказчику. */
    private static final List<String> PUBLIC_ONLY = List.of(Visibility.PUBLIC.name());
    /** Что показываем команде. */
    private static final List<String> EVERYTHING =
        Arrays.stream(Visibility.values()).map(Enum::name).toList();

    @Inject UserService userService;

    /** Событие с видимостью по умолчанию — {@link Visibility#PUBLIC}. */
    @Transactional
    public void record(Integer projectId, Integer taskId, Integer userId,
                       ActivityAction action, Map<String, Object> details) {
        record(projectId, taskId, userId, action, details, Visibility.PUBLIC);
    }

    @Transactional
    public void record(Integer projectId, Integer taskId, Integer userId,
                       ActivityAction action, Map<String, Object> details, Visibility visibility) {
        var entity = new ActivityLogEntity();
        entity.projectId = projectId;
        entity.taskId = taskId;
        entity.userId = userId;
        entity.actionType = action.name();
        entity.actionDetails = serialize(details);
        entity.visibility = (visibility != null ? visibility : Visibility.PUBLIC).name();
        entity.createdAt = Instant.now().toEpochMilli();
        entity.persist();
    }

    /**
     * @param includeInternal видит ли вызывающий внутренние события; {@code false}
     *        для заказчика — тогда в ленту попадают только {@code PUBLIC}-записи.
     *        Фильтр в запросе, а не после выборки: иначе постраничность отдавала бы
     *        меньше {@code limit} строк.
     */
    public List<ActivityResponse> listByProject(Integer projectId, int limit, int offset, boolean includeInternal) {
        return toResponses(ActivityLogEntity.<ActivityLogEntity>find(
                "projectId = ?1 and visibility in ?2 order by createdAt desc, id desc",
                projectId, visibilityScope(includeInternal))
            .range(offset, offset + limit - 1)
            .list());
    }

    public List<ActivityResponse> listByTask(Integer taskId, int limit, int offset, boolean includeInternal) {
        return toResponses(ActivityLogEntity.<ActivityLogEntity>find(
                "taskId = ?1 and visibility in ?2 order by createdAt desc, id desc",
                taskId, visibilityScope(includeInternal))
            .range(offset, offset + limit - 1)
            .list());
    }

    private static List<String> visibilityScope(boolean includeInternal) {
        return includeInternal ? EVERYTHING : PUBLIC_ONLY;
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
            Visibility.fromString(entity.visibility),
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
