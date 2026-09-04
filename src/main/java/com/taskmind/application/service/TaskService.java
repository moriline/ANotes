package com.taskmind.application.service;

import com.taskmind.api.dto.TaskUpdateRequest;
import com.taskmind.domain.model.ActivityAction;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.Instant;

@ApplicationScoped
public class TaskService {

    @Inject TaskRepository repository;
    @Inject H2TimeEntryRepository timeEntryRepository;
    @Inject ProjectStatusService projectStatusService;
    @Inject ActivityLogService activityLog;

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
        var saved = repository.save(task);

        var details = new HashMap<String, Object>();
        details.put("title", saved.title());
        activityLog.record(saved.projectId(), saved.id(), creatorUserId, ActivityAction.TASK_CREATED, details);

        return saved;
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
    public Task applyUpdate(Task current, TaskUpdateRequest req, Integer actorUserId) {
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
        var saved = repository.save(updated);
        recordUpdate(current, saved, actorUserId);
        return saved;
    }

    /**
     * Смена исполнителя и перевод по статусам попадают в ленту отдельными
     * событиями — в ленте это то, что читают в первую очередь. Общий TASK_UPDATED
     * пишется, только если поменялось что-то ещё, иначе один PATCH давал бы две
     * записи об одном и том же.
     */
    private void recordUpdate(Task before, Task after, Integer actorUserId) {
        if (!Objects.equals(before.assignedUserId(), after.assignedUserId())) {
            var details = new HashMap<String, Object>();
            details.put("from", before.assignedUserId());
            details.put("to", after.assignedUserId());
            activityLog.record(after.projectId(), after.id(), actorUserId, ActivityAction.ASSIGNEE_UPDATED, details);
        }
        if (!Objects.equals(before.statusId(), after.statusId())) {
            var details = new HashMap<String, Object>();
            details.put("from", before.statusId());
            details.put("to", after.statusId());
            activityLog.record(after.projectId(), after.id(), actorUserId, ActivityAction.STATUS_CHANGED, details);
        }

        var changedFields = new ArrayList<String>();
        if (!Objects.equals(before.title(), after.title())) changedFields.add("title");
        if (!Objects.equals(before.description(), after.description())) changedFields.add("description");
        if (!Objects.equals(before.tags(), after.tags())) changedFields.add("tags");
        if (!Objects.equals(before.dueDate(), after.dueDate())) changedFields.add("dueDate");
        if (!Objects.equals(before.startDate(), after.startDate())) changedFields.add("startDate");
        if (!Objects.equals(before.estimatedHours(), after.estimatedHours())) changedFields.add("estimatedHours");
        if (before.isArchived() != after.isArchived()) changedFields.add("isArchived");

        if (!changedFields.isEmpty()) {
            var details = new HashMap<String, Object>();
            details.put("fields", changedFields);
            activityLog.record(after.projectId(), after.id(), actorUserId, ActivityAction.TASK_UPDATED, details);
        }
    }

    public List<Task> search(TaskSearchCriteria criteria) {
        return repository.search(criteria);
    }

    public long countMatching(TaskSearchCriteria criteria) {
        return repository.count(criteria);
    }

    /**
     * Записывает итог по задаче — то место, куда модель кладёт результат, решив
     * задачу. Пустая строка стирает прежний итог.
     */
    @Transactional
    public Task setSummary(Integer taskId, String summary, Integer actorUserId) {
        repository.updateSummary(taskId, summary);
        Task updated = repository.findById(taskId).orElseThrow();

        var details = new HashMap<String, Object>();
        details.put("length", summary == null ? 0 : summary.length());
        activityLog.record(updated.projectId(), updated.id(), actorUserId, ActivityAction.SUMMARY_UPDATED, details);

        return updated;
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
    public Task addDiscussionBlock(Task task, DiscussionBlock block, Integer actorUserId) {
        var blocks = new ArrayList<>(task.discussion());
        blocks.add(block);
        Task updated = storeDiscussion(task, blocks);

        var details = new HashMap<String, Object>();
        details.put("author", block.author());
        details.put("type", block.type() != null ? block.type().name() : null);
        activityLog.record(task.projectId(), task.id(), actorUserId, ActivityAction.DISCUSSION_BLOCK_ADDED, details);

        return updated;
    }

    /** Полная замена дерева обсуждения — правка и перестройка уже написанного. */
    @Transactional
    public Task replaceDiscussion(Task task, List<DiscussionBlock> blocks, Integer actorUserId) {
        Task updated = storeDiscussion(task, blocks);

        var details = new HashMap<String, Object>();
        details.put("blocks", blocks.size());
        activityLog.record(task.projectId(), task.id(), actorUserId, ActivityAction.DISCUSSION_REPLACED, details);

        return updated;
    }

    private Task storeDiscussion(Task task, List<DiscussionBlock> blocks) {
        repository.updateDiscussion(task.id(), blocks);
        return repository.findById(task.id()).orElseThrow();
    }

    @Transactional
    public void deleteTask(Integer taskId) {
        repository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        repository.deleteById(taskId);
    }

    /**
     * Списывает время. {@code startTimeMillis} — момент, к которому относится
     * работа; {@code null} означает «сейчас» (запись текущим временем).
     */
    @Transactional
    public TimeEntry logTime(Integer taskId, Integer userId, long seconds, String description, Long startTimeMillis) {
        Instant startTime = startTimeMillis != null ? Instant.ofEpochMilli(startTimeMillis) : Instant.now();
        var entry = new TimeEntry(null, taskId, userId, seconds, description, startTime, null);
        return timeEntryRepository.save(entry);
    }

    public Long getTotalTimeForTask(Integer taskId) {
        return timeEntryRepository.sumSecondsByTaskId(taskId);
    }
}
