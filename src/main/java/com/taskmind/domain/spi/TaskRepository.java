package com.taskmind.domain.spi;

import com.taskmind.domain.model.Task;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(UUID id);
    List<Task> findByProject(UUID projectId);
    void deleteById(UUID id);
}
