package com.taskmind.application.service;

/**
 * Запрос конфликтует с тем, что уже есть: занятый username или email, занятое
 * имя проекта.
 *
 * <p>Раньше здесь был {@link IllegalArgumentException}, маппера на неё нет, и
 * клиент получал 500 — по такому ответу форма регистрации не может сказать
 * «этот логин занят», потому что 500 неотличима от настоящей поломки сервера.
 *
 * <p>Живёт в слое приложения и ничего не знает о JAX-RS; в 409 его превращает
 * {@code DuplicateResourceExceptionMapper}.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
