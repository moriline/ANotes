package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Task;
import com.taskmind.domain.spi.SortDirection;
import com.taskmind.domain.spi.TaskRepository;
import com.taskmind.domain.spi.TaskSearchCriteria;
import com.taskmind.domain.spi.TaskSortField;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2TaskRepository implements TaskRepository {

    @Override
    @Transactional
    public Task save(Task task) {
        if (task.id() != null) {
            TaskEntity entity = TaskEntity.findById(task.id());
            if (entity != null) {
                entity.projectId = task.projectId();
                entity.title = task.title();
                entity.description = task.description();
                entity.creatorUserId = task.creatorUserId();
                entity.assignedUserId = task.assignedUserId();
                entity.statusId = task.statusId();
                entity.dueDate = task.dueDate();
                entity.startDate = task.startDate();
                entity.estimatedHours = task.estimatedHours();
                entity.tags = task.tags() == null || task.tags().isEmpty() 
                    ? "[]" 
                    : "[\"" + String.join("\",\"", task.tags()) + "\"]";
                entity.discussion = task.discussion();
                entity.summary = task.summary();
                entity.isArchived = task.isArchived();
                entity.updatedAt = java.time.Instant.now().toEpochMilli();
                return entity.toDomainModel();
            }
        }
        
        var entity = TaskEntity.fromDomain(task);
        entity.persist();
        return entity.toDomainModel();
    }

    @Override
    public Optional<Task> findById(Integer id) {
        return TaskEntity.<TaskEntity>findByIdOptional(id).map(TaskEntity::toDomainModel);
    }

    @Override
    public List<Task> findByProject(Integer projectId) {
        return TaskEntity.<TaskEntity>list("projectId", projectId)
                .stream().map(TaskEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        TaskEntity.deleteById(id);
    }

    @Override
    public List<Task> findByDiscussionContent(String query) {
        // Use native query because the 'discussion' field is mapped to a List and JPQL LIKE doesn't work directly on it
        var queryObj = TaskEntity.getEntityManager()
                .createNativeQuery("SELECT * FROM tasks WHERE discussion LIKE :query", TaskEntity.class)
                .setParameter("query", "%" + query + "%");
        
        List<TaskEntity> resultList = queryObj.getResultList();
        
        return resultList.stream()
                .map(TaskEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateDiscussion(Integer taskId, List<com.taskmind.domain.model.DiscussionBlock> discussion) {
        TaskEntity entity = TaskEntity.findById(taskId);
        if (entity != null) {
            entity.discussion = discussion;
            entity.updatedAt = java.time.Instant.now().toEpochMilli();
        }
    }

    @Override
    @Transactional
    public void updateSummary(Integer taskId, String summary) {
        TaskEntity entity = TaskEntity.findById(taskId);
        if (entity != null) {
            entity.summary = summary;
            entity.updatedAt = java.time.Instant.now().toEpochMilli();
        }
    }

    /**
     * Поиск нативным SQL, а не через Panache/JPQL: {@code discussion} отображён на
     * {@code List<DiscussionBlock>} через конвертер, и LIKE по нему из JPQL не
     * работает — параметр попытались бы прогнать через тот же конвертер.
     */
    @Override
    public List<Task> search(TaskSearchCriteria criteria) {
        if (criteria.projectIds() == null || criteria.projectIds().isEmpty()) {
            return List.of();
        }

        var params = new HashMap<String, Object>();
        String where = buildWhere(criteria, params);

        // taskId в конце — устойчивый порядок: без него страницы «плывут», когда у
        // задач совпадает время изменения.
        String sql = "SELECT * FROM tasks WHERE " + where
            + " ORDER BY " + sortField(criteria).column() + " " + sortDirection(criteria).name()
            + ", taskId DESC";

        var query = TaskEntity.getEntityManager().createNativeQuery(sql, TaskEntity.class);
        params.forEach(query::setParameter);
        query.setFirstResult(criteria.offset());
        query.setMaxResults(criteria.limit());

        List<TaskEntity> rows = query.getResultList();
        return rows.stream().map(TaskEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public long count(TaskSearchCriteria criteria) {
        if (criteria.projectIds() == null || criteria.projectIds().isEmpty()) {
            return 0;
        }

        var params = new HashMap<String, Object>();
        String where = buildWhere(criteria, params);

        var query = TaskEntity.getEntityManager()
            .createNativeQuery("SELECT COUNT(*) FROM tasks WHERE " + where);
        params.forEach(query::setParameter);

        return ((Number) query.getSingleResult()).longValue();
    }

    private String buildWhere(TaskSearchCriteria criteria, Map<String, Object> params) {
        var where = new StringBuilder("projectId IN (:projectIds)");
        params.put("projectIds", criteria.projectIds());

        if (isFilled(criteria.titleSearch())) {
            where.append(" AND title LIKE :title");
            params.put("title", "%" + criteria.titleSearch() + "%");
        }
        if (isFilled(criteria.contentSearch())) {
            // Весь текст задачи разом. По discussion идёт LIKE по сырому JSON, поэтому
            // сюда же попадают имена авторов и названия типов блоков.
            where.append(" AND (LOWER(title) LIKE :content OR LOWER(description) LIKE :content")
                 .append(" OR LOWER(summary) LIKE :content OR LOWER(discussion) LIKE :content)");
            params.put("content", "%" + criteria.contentSearch().toLowerCase() + "%");
        }
        if (criteria.assignedUserId() != null) {
            where.append(" AND assignedUserId = :assignedUserId");
            params.put("assignedUserId", criteria.assignedUserId());
        }
        if (criteria.statusId() != null) {
            where.append(" AND statusId = :statusId");
            params.put("statusId", criteria.statusId());
        }
        if (criteria.isArchived() != null) {
            where.append(" AND isArchived = :isArchived");
            params.put("isArchived", criteria.isArchived());
        }
        return where.toString();
    }

    private static TaskSortField sortField(TaskSearchCriteria criteria) {
        return criteria.sortField() != null ? criteria.sortField() : TaskSortField.UPDATED_AT;
    }

    private static SortDirection sortDirection(TaskSearchCriteria criteria) {
        return criteria.sortDirection() != null ? criteria.sortDirection() : SortDirection.DESC;
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }
}
