package com.taskmind.domain.spi;

import com.taskmind.domain.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(Integer id);
    List<Task> findByProject(Integer projectId);
    void deleteById(Integer id);
}
