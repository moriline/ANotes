package com.taskmind.api.dto;

import com.taskmind.domain.model.Visibility;

import java.time.Instant;
import java.util.Map;

/**
 * Событие в ленте проекта. {@code actionType} отдаётся строкой, а не enum'ом:
 * в базе уже лежат записи из сида, и неизвестный тип не должен ронять чтение ленты.
 */
public record ActivityResponse(
    Integer id,
    Integer projectId,
    Integer taskId,
    Integer userId,
    String username,
    String actionType,
    Map<String, Object> details,
    Visibility visibility,
    Instant createdAt
) {}
