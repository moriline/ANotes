package com.taskmind.api.dto;

import java.util.List;

/**
 * Частичное обновление задачи: {@code null} в поле означает «не трогать».
 *
 * <p>Из-за этого через PATCH нельзя снять уже проставленное значение (например,
 * убрать исполнителя) — присланный null неотличим от отсутствующего поля. Для
 * демо-сценария этого достаточно; если понадобится очистка, поля придётся
 * обернуть в Optional и включить jackson-datatype-jdk8.
 */
public record TaskUpdateRequest(
    String title,
    String description,
    List<String> tags,
    Integer assignedUserId,
    Integer statusId,
    Long dueDate,
    Long startDate,
    Double estimatedHours,
    Boolean isArchived
) {}
