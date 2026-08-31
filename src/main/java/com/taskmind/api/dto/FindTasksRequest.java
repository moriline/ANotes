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
    public Integer statusId;
    public Boolean isArchived;
}
