package com.taskmind.api.dto;

import com.taskmind.domain.model.BlockType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Новый блок обсуждения задачи.
 *
 * @param id       необязательный идентификатор, сгенерированный на стороне клиента;
 *                 если не задан, его выдаёт сервер
 * @param parentId блок, ответом на который является этот; уровень вложенности
 *                 сервер считает сам, поэтому в запросе его нет
 * @param author   свободная строка, а не userId — сюда пишется имя агента
 *                 (например {@code claude}); если пусто, подставляется username
 *                 вызывающего
 * @param type     тип блока; по умолчанию {@code MESSAGE}. Для вывода модели
 *                 обычно нужен {@code DECISION}
 * @param content  текст блока
 */
public record DiscussionBlockRequest(
    UUID id,
    UUID parentId,
    String author,
    BlockType type,

    @NotBlank(message = "content is required")
    String content
) {}
