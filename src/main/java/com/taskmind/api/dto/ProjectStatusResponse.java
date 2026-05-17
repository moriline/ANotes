package com.taskmind.api.dto;

import com.taskmind.infrastructure.db.ProjectStatusEntity;

public record ProjectStatusResponse(
    Integer id,
    Integer projectId,
    String statusName,
    String statusColor,
    Integer statusOrder,
    boolean isDefault,
    boolean isClosed
) {
    public static ProjectStatusResponse fromEntity(ProjectStatusEntity e) {
        return new ProjectStatusResponse(e.id, e.projectId, e.statusName, e.statusColor, e.statusOrder, e.isDefault, e.isClosed);
    }
}
