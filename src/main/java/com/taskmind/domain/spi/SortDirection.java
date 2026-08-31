package com.taskmind.domain.spi;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection fromValue(String value) {
        for (SortDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown sort direction: " + value);
    }
}
