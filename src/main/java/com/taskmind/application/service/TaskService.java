package com.taskmind.application.service;

import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.domain.model.DiscussionBlock;
import com.taskmind.domain.model.Task;
import com.taskmind.domain.model.TimeEntry;
import com.taskmind.domain.spi.TaskRepository;
import com.taskmind.domain.spi.TaskSearchCriteria;
import com.taskmind.infrastructure.db.H2TimeEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

@ApplicationScoped
public class TaskService {

    @Inject TaskRepository repository;
    @Inject H2TimeEntryRepository timeEntryRepository;
    @Inject ProjectStatusService projectStatusService;

    @Transactional
    public Task createTask(Integer projectId, String title, String description, List<String> tags, Integer creatorUserId) {
        // Новая задача встаёт в первую колонку доски проекта; null остаётся только
        // у проектов, созданных до появления статусов по умолчанию.
        Integer statusId = projectStatusService.findDefaultStatus(projectId)
            .map(status -> status.id)
            .orElse(null);

        var task = new Task(
            null,
            projectId,
            title,
            description,
            creatorUserId,
            null,
            statusId,
            null,
            null,
            null,
            tags != null ? tags : List.of(),
            false,
            List.of(),
            null,
            Instant.now(),
            Instant.now()
        );
        return repository.save(task);
    }

    public List<Task> listByProject(Integer projectId) {
        return repository.findByProject(projectId);
    }

    public Optional<Task> findById(Integer taskId) {
        return repository.findById(taskId);
    }

    /**
     * Накладывает частичное обновление на задачу: поле со значением {@code null}
     * остаётся прежним. Обсуждение, summary и время создания не трогаются — их
     * меняют другие сценарии.
     */
    @Transactional
    public Task applyUpdate(Task current, TaskUpdateRequest req) {
        var updated = new Task(
            current.id(),
            current.projectId(),
            req.title() != null ? req.title() : current.title(),
            req.description() != null ? req.description() : current.description(),
            current.creatorUserId(),
            req.assignedUserId() != null ? req.assignedUserId() : current.assignedUserId(),
            req.statusId() != null ? req.statusId() : current.statusId(),
            req.dueDate() != null ? req.dueDate() : current.dueDate(),
            req.startDate() != null ? req.startDate() : current.startDate(),
            req.estimatedHours() != null ? req.estimatedHours() : current.estimatedHours(),
            req.tags() != null ? req.tags() : current.tags(),
            req.isArchived() != null ? req.isArchived() : current.isArchived(),
            current.discussion(),
            current.summary(),
            current.createdAt(),
            Instant.now()
        );
        return repository.save(updated);
    }

    public List<Task> search(TaskSearchCriteria criteria) {
        return repository.search(criteria);
    }

    /**
     * Записывает итог по задаче — то место, куда модель кладёт результат, решив
     * задачу. Пустая строка стирает прежний итог.
     */
    @Transactional
    public Task setSummary(Integer taskId, String summary) {
        repository.updateSummary(taskId, summary);
        return repository.findById(taskId).orElseThrow();
    }

    /**
     * Добавляет блок в конец обсуждения задачи.
     *
     * <p>Обсуждение лежит одним JSON-полем, поэтому это чтение-изменение-запись:
     * два одновременных добавления в одну задачу могут затереть друг друга.
     * Для потока «агент дописывает свои выводы» этого достаточно, для настоящей
     * многопользовательской нагрузки блоки нужно вынести в отдельную таблицу.
     */
    @Transactional
    public Task addDiscussionBlock(Task task, DiscussionBlock block) {
        var blocks = new ArrayList<>(task.discussion());
        blocks.add(block);
        repository.updateDiscussion(task.id(), blocks);
        return repository.findById(task.id()).orElseThrow();
    }

    @Transactional
    public void deleteTask(Integer taskId) {
        repository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        repository.deleteById(taskId);
    }

    @Transactional
    public TimeEntry logTime(Integer taskId, Integer userId, long seconds, String description) {
        var entry = new TimeEntry(null, taskId, userId, seconds, description, Instant.now(), null);
        return timeEntryRepository.save(entry);
    }

    public Long getTotalTimeForTask(Integer taskId) {
        return timeEntryRepository.sumSecondsByTaskId(taskId);
    }
}
