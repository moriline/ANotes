package com.taskmind.domain.spi;

import com.taskmind.domain.model.DiscussionBlock;
import com.taskmind.domain.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(Integer id);
    List<Task> findByProject(Integer projectId);
    void deleteById(Integer id);
    List<Task> findByDiscussionContent(String query);
    List<Task> search(TaskSearchCriteria criteria);

    /** Сколько задач подходит под условия без учёта limit/offset. */
    long count(TaskSearchCriteria criteria);

    /** Точечная запись итога по задаче, без перезаписи остальных полей. */
    void updateSummary(Integer taskId, String summary);

    /** Точечная замена всего дерева обсуждения. */
    void updateDiscussion(Integer taskId, List<DiscussionBlock> discussion);
}
