package com.taskmind.application.service;

import com.taskmind.domain.model.Task;
import com.taskmind.domain.spi.TaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.time.Instant;

@ApplicationScoped
public class TaskService {

    @Inject TaskRepository repository;

    @Transactional
    public Task createTask(Integer projectId, String title, String description, List<String> tags, Integer creatorUserId) {
        var task = new Task(
            null,
            projectId,
            title,
            description,
            creatorUserId,
            null,
            null,
            null,
            null,
            null,
            tags != null ? tags : List.of(),
            false,
            Instant.now(),
            Instant.now()
        );
        return repository.save(task);
    }

    public List<Task> listByProject(Integer projectId) {
        return repository.findByProject(projectId);
    }

    @Transactional
    public void deleteTask(Integer taskId) {
        repository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        repository.deleteById(taskId);
    }
}
