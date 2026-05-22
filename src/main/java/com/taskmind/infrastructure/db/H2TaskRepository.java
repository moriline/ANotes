package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Task;
import com.taskmind.domain.spi.TaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
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

    @Transactional
    public void updateDiscussion(Integer taskId, List<com.taskmind.domain.model.DiscussionBlock> discussion) {
        TaskEntity entity = TaskEntity.findById(taskId);
        if (entity != null) {
            entity.discussion = discussion;
            entity.updatedAt = java.time.Instant.now().toEpochMilli();
        }
    }

    @Transactional
    public void updateSummary(Integer taskId, String summary) {
        TaskEntity entity = TaskEntity.findById(taskId);
        if (entity != null) {
            entity.summary = summary;
            entity.updatedAt = java.time.Instant.now().toEpochMilli();
        }
    }
}
