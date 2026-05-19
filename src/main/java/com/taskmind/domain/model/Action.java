package com.taskmind.domain.model;

public enum Action {
    TASK_CREATE("task:create"),
    TASK_READ("task:read"),
    TASK_UPDATE("task:update"),
    TASK_DELETE("task:delete"),
    TASK_ASSIGN("task:assign"),
    PROJECT_READ("project:read"),
    PROJECT_CREATE("project:create"),
    PROJECT_UPDATE("project:update"),
    PROJECT_DELETE("project:delete"),
    PROJECT_MANAGE_MEMBERS("project:manage_members"),
    USER_MANAGE("user:manage"),
    ROLE_MANAGE("role:manage");

    private final String value;

    Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Action fromValue(String value) {
        for (Action action : Action.values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action: " + value);
    }
}
