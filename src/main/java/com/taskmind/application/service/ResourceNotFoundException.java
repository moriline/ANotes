package com.taskmind.application.service;

/**
 * Объект, к которому обратились по id, не существует.
 *
 * <p>Раньше на его месте был {@link IllegalArgumentException}, маппера на неё
 * нет, и клиент получал 500 — «сервер сломался» вместо «такого нет». Соседний
 * delete в тех же сервисах молча отвечал 204, будто удаление удалось.
 *
 * <p>Живёт в слое приложения и ничего не знает о JAX-RS; в 404 его превращает
 * {@code ResourceNotFoundExceptionMapper}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
