package com.taskmind.application.service;

import com.taskmind.domain.model.Task;
import com.taskmind.domain.spi.TaskRepository;
import com.taskmind.infrastructure.ai.RagTaskIndexer;
import com.taskmind.infrastructure.storage.FileSystemTaskSync;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TaskService {

    @Inject TaskRepository repository;
    @Inject FileSystemTaskSync fsSync;
    @Inject RagTaskIndexer ragIndexer;

    @Transactional
    public Task createTask(UUID projectId, String title, String description, List<String> tags) {
        var task = Task.create(projectId, title, description, tags);
        repository.save(task);
        try { fsSync.syncToDisk(task); } catch (Exception e) { System.err.println("FS sync failed: " + e.getMessage()); }
        ragIndexer.indexTask(task.id(), projectId, task.title() + "\n" + task.description());
        return task;
    }

    public List<Task> listByProject(UUID projectId) {
        return repository.findByProject(projectId);
    }

    public List<Task> searchByProject(UUID projectId, String query) {
        var ids = ragIndexer.searchTaskIds(projectId, query, 5);
        if (ids.isEmpty()) return List.of();
        return ids.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(UUID::fromString)
            .map(repository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        var task = repository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        try { fsSync.deleteFromDisk(task); } catch (Exception e) { System.err.println("FS delete failed: " + e.getMessage()); }
        repository.deleteById(taskId);
    }
}
