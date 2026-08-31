package com.taskmind.domain.spi;

/**
 * Разрешённые поля сортировки задач.
 *
 * <p>Это whitelist, а не просто перечисление: поиск собирается нативным SQL, и
 * подставлять в ORDER BY строку из запроса напрямую нельзя.
 */
public enum TaskSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    DUE_DATE("dueDate"),
    TITLE("title");

    private final String column;

    TaskSortField(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }

    /** Принимает и имя поля из API ({@code dueDate}), и имя константы ({@code DUE_DATE}). */
    public static TaskSortField fromValue(String value) {
        for (TaskSortField field : values()) {
            if (field.column.equalsIgnoreCase(value) || field.name().equalsIgnoreCase(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unknown sort field: " + value);
    }
}
