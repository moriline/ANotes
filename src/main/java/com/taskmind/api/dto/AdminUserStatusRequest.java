package com.taskmind.api.dto;

/**
 * Тело {@code PUT /api/admin/users/{id}/status}: блокировка и разблокировка
 * учётной записи. {@code isActive} обязателен — пустое тело отклоняется, чтобы
 * кривой запрос не трактовался как «заблокировать».
 */
public record AdminUserStatusRequest(Boolean isActive) {}
