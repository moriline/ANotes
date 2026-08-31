package com.taskmind.application.service;

/**
 * Вызывающий аутентифицирован, но не имеет права на конкретный объект — чужой
 * комментарий, чужой файл.
 *
 * <p>Раньше на это место кидали {@link SecurityException}, а её маппер отдаёт
 * 401: клиенту сообщалось «ты не залогинен» вместо «тебе нельзя», и он уходил
 * перелогиниваться по кругу. Отдельный тип нужен именно чтобы отличить этот
 * случай от неверных учётных данных, где 401 как раз правильный ответ.
 *
 * <p>Исключение живёт в слое приложения и ничего не знает о JAX-RS; в 403 его
 * превращает {@code AccessDeniedExceptionMapper}.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
