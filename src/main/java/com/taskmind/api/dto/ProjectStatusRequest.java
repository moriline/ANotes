package com.taskmind.api.dto;

public record ProjectStatusRequest(
    Integer projectId,
    String statusName,
    String statusColor,
    Integer statusOrder,
    boolean isDefault,
    boolean isClosed
) {}
