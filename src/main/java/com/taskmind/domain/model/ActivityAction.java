package com.taskmind.domain.model;

/**
 * Что именно произошло в проекте. Хранится строкой в {@code activityLog.actionType}.
 *
 * <p>Названия ASSIGNEE_UPDATED, STATUS_CHANGED, TASK_CREATED и COMMENT_ADDED взяты
 * из сида: там уже лежат записи с такими типами, и ломать их вокабуляр незачем.
 */
public enum ActivityAction {
    PROJECT_CREATED,
    PROJECT_UPDATED,
    TASK_CREATED,
    TASK_UPDATED,
    ASSIGNEE_UPDATED,
    STATUS_CHANGED,
    SUMMARY_UPDATED,
    DISCUSSION_BLOCK_ADDED,
    DISCUSSION_REPLACED,
    MEMBER_ADDED,
    MEMBER_ROLE_CHANGED,
    MEMBER_REMOVED,
    COMMENT_ADDED,
    COMMENT_EDITED,
    COMMENT_DELETED
}
