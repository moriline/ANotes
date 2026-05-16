package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Task;
import com.taskmind.domain.spi.TaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2TaskRepository implements TaskRepository {

    @Override
    @Transactional
    public Task save(Task task) {
        var entity = TaskEntity.fromDomain(task);
        entity.persist();
        return entity.toDomainModel();
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return TaskEntity.<TaskEntity>find("id", id).firstResultOptional()
            .map(TaskEntity::toDomainModel);
    }

    @Override
    public List<Task> findByProject(UUID projectId) {
        return TaskEntity.<TaskEntity>list("projectId", projectId)
            .stream().map(TaskEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        TaskEntity.deleteById(id);
    }
}
