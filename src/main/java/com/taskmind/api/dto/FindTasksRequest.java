package com.taskmind.api.dto;

public class FindTasksRequest {
    public Integer projectId;
    public String titleSearch;

    /**
     * Подстрока в любом тексте задачи: заголовок, описание, summary и обсуждение.
     * Без учёта регистра. По обсуждению поиск идёт по сырому JSON, поэтому находит
     * и по автору блока, и по его типу (например {@code DECISION}).
     */
    public String contentSearch;

    public Integer assignedUserId;

    /**
     * «Мои задачи»: подставляет вызывающего в фильтр по исполнителю, чтобы клиенту
     * не приходилось сначала спрашивать свой id. Вместе с {@code assignedUserId}
     * можно указывать, только если там тот же самый пользователь.
     */
    public Boolean assignedToMe;

    public Integer statusId;
    public Boolean isArchived;

    /** Поле сортировки: createdAt, updatedAt, dueDate или title. По умолчанию updatedAt. */
    public String sortBy;

    /** asc или desc. По умолчанию desc. */
    public String sortDir;

    /** Размер страницы: по умолчанию 50, максимум 200. */
    public Integer limit;

    /** Сколько записей пропустить. По умолчанию 0. */
    public Integer offset;
}
