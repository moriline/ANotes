package com.taskmind.domain.spi;

import java.util.Collection;

/**
 * Условия поиска задач. Все поля фильтра, кроме {@code projectIds}, необязательны:
 * {@code null} означает «не фильтровать по этому признаку». Заданные условия
 * складываются по И.
 *
 * @param projectIds     проекты, в которых вообще разрешено искать (обязательно)
 * @param titleSearch    подстрока заголовка, с учётом регистра
 * @param contentSearch  подстрока в любом тексте задачи: заголовок, описание,
 *                       summary и обсуждение; без учёта регистра
 * @param assignedUserId исполнитель
 * @param statusId       статус
 * @param isArchived     архивная или нет
 * @param sortField      поле сортировки
 * @param sortDirection  направление сортировки
 * @param limit          сколько записей вернуть
 * @param offset         сколько записей пропустить
 */
public record TaskSearchCriteria(
    Collection<Integer> projectIds,
    String titleSearch,
    String contentSearch,
    Integer assignedUserId,
    Integer statusId,
    Boolean isArchived,
    TaskSortField sortField,
    SortDirection sortDirection,
    int limit,
    int offset
) {}
