package com.taskmind.api.rest;

import jakarta.ws.rs.BadRequestException;

/**
 * Разбор limit/offset. Вынесено отдельно, чтобы во всех листингах эти параметры
 * вели себя одинаково и отдавали внятный 400, а не 500 из глубины сервиса.
 */
final class PagingParams {

    private PagingParams() {}

    /** {@code null} — значение по умолчанию; слишком большое значение обрезается до максимума. */
    static int limit(Integer requested, int defaultLimit, int maxLimit) {
        if (requested == null) {
            return defaultLimit;
        }
        if (requested <= 0) {
            throw new BadRequestException("limit должен быть положительным");
        }
        return Math.min(requested, maxLimit);
    }

    static int offset(Integer requested) {
        if (requested == null) {
            return 0;
        }
        if (requested < 0) {
            throw new BadRequestException("offset не может быть отрицательным");
        }
        return requested;
    }
}
