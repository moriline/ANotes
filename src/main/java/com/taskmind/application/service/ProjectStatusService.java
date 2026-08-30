package com.taskmind.application.service;

import com.taskmind.infrastructure.db.ProjectStatusEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Статусы задач внутри проекта.
 *
 * <p>Новый проект раньше создавался вообще без статусов: колонка задач
 * {@code statusId} оставалась пустой, доску показать было нечем, а завести статус
 * можно было только руками через {@code POST /api/project-statuses}. Теперь любой
 * проект сразу получает доску «To Do → In Progress → Done».
 */
@ApplicationScoped
public class ProjectStatusService {

    /** Доска по умолчанию для только что созданного проекта. */
    private static final List<DefaultStatus> DEFAULTS = List.of(
        new DefaultStatus("To Do",       "#95A5A6", 0, true,  false),
        new DefaultStatus("In Progress", "#3498DB", 1, false, false),
        new DefaultStatus("Done",        "#2ECC71", 2, false, true)
    );

    private record DefaultStatus(String name, String color, int order, boolean isDefault, boolean isClosed) {}

    @Transactional
    public List<ProjectStatusEntity> createDefaults(Integer projectId) {
        return DEFAULTS.stream()
            .map(d -> create(projectId, d.name(), d.color(), d.order(), d.isDefault(), d.isClosed()))
            .toList();
    }

    @Transactional
    public ProjectStatusEntity create(Integer projectId, String statusName, String statusColor,
                                      Integer statusOrder, boolean isDefault, boolean isClosed) {
        var entity = new ProjectStatusEntity();
        entity.projectId = projectId;
        entity.statusName = statusName;
        entity.statusColor = statusColor;
        entity.statusOrder = statusOrder;
        entity.isDefault = isDefault;
        entity.isClosed = isClosed;
        entity.persist();
        return entity;
    }

    /** Статусы проекта в порядке колонок доски. */
    public List<ProjectStatusEntity> listByProject(Integer projectId) {
        return ProjectStatusEntity.list("projectId = ?1 order by statusOrder", projectId);
    }

    /**
     * Статус, в который попадает только что созданная задача: помеченный
     * {@code isDefault}, иначе первый по порядку. Пусто, если у проекта нет
     * статусов вовсе — такие проекты остались от версий до этой доски.
     */
    public Optional<ProjectStatusEntity> findDefaultStatus(Integer projectId) {
        List<ProjectStatusEntity> statuses = listByProject(projectId);
        return statuses.stream()
            .filter(s -> s.isDefault)
            .findFirst()
            .or(() -> statuses.stream().findFirst());
    }
}
