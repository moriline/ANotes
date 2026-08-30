package com.taskmind.infrastructure.db;

import com.taskmind.TestDataCleanup;
import com.taskmind.domain.model.BlockType;
import com.taskmind.domain.model.DiscussionBlock;
import com.taskmind.domain.model.Task;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TaskDiscussionRepositoryTest {

    @Inject
    H2TaskRepository repository;

    @Inject
    TestDataCleanup cleanup;

    /**
     * Тест вставляет задачи напрямую в projectId=1 с creatorUserId=1, поэтому эти
     * записи должны существовать. Заодно сброс изолирует методы друг от друга:
     * поиск по обсуждениям проверяет точные размеры выборок.
     */
    @BeforeEach
    void resetData() {
        cleanup.clearAll();
    }

    @Test
    public void shouldSaveAndLoadTaskWithFiveDiscussionBlocks() {
        Task task = Task.create(1, "Task with 5 blocks", 1);
        
        List<DiscussionBlock> blocks = List.of(
            createBlock("Alice", "Hello everyone", BlockType.MESSAGE, 0, null),
            createBlock("Bob", "Hi Alice, what's the status?", BlockType.QUESTION, 1, null),
            createBlock("Alice", "We are almost done with the API", BlockType.MESSAGE, 2, null),
            createBlock("Charlie", "I found a bug in the auth flow", BlockType.MESSAGE, 0, null),
            createBlock("Bob", "Fix it before the demo", BlockType.DECISION, 1, null)
        );
        
        Task taskWithDiscussion = new Task(
            null, 1, "Task with 5 blocks", null, 1, null, null, null, null, null,
            List.of(), false, blocks, "Demo ready", Instant.now(), Instant.now()
        );

        Task savedTask = repository.save(taskWithDiscussion);
        assertNotNull(savedTask.id());
        
        Task loadedTask = repository.findById(savedTask.id()).orElseThrow();
        assertEquals(5, loadedTask.discussion().size());
        assertEquals("Alice", loadedTask.discussion().get(0).author());
        assertEquals("Fix it before the demo", loadedTask.discussion().get(4).content());
    }

    @Test
    public void shouldSearchInDiscussionJsonByDifferentCriteria() {
        // Prepare test data
        Task task1 = createTaskWithDiscussion("Alice", "Secret code is 1234", "System A");
        Task task2 = createTaskWithDiscussion("Bob", "Meeting at 5pm", "Project X");
        Task task3 = createTaskWithDiscussion("Charlie", "Bug in production", "Critical fix");
        Task task4 = createTaskWithDiscussion("Alice", "Review the PR", "Documentation");

        repository.save(task1);
        repository.save(task2);
        repository.save(task3);
        repository.save(task4);

        // 1. Search by Author
        List<Task> byAuthor = repository.findByDiscussionContent("Bob");
        assertFalse(byAuthor.isEmpty());
        assertTrue(byAuthor.stream().anyMatch(t -> t.discussion().get(0).author().equals("Bob")));

        // 2. Search by Content substring
        List<Task> byContent = repository.findByDiscussionContent("Secret code");
        assertEquals(1, byContent.size());
        assertEquals("Alice", byContent.get(0).discussion().get(0).author());

        // 3. Search by Block Type (it's part of JSON)
        List<Task> byType = repository.findByDiscussionContent("MESSAGE");
        assertTrue(byType.size() >= 4);

        // 4. Search by specific word
        List<Task> byPartialInfo = repository.findByDiscussionContent("production");
        assertEquals(1, byPartialInfo.size());
        assertEquals("Charlie", byPartialInfo.get(0).discussion().get(0).author());
    }

    @Test
    public void shouldPreserveClientGeneratedUUID() {
        UUID clientSideId = UUID.randomUUID();
        DiscussionBlock block = new DiscussionBlock(
            clientSideId,
            null,
            "Client",
            BlockType.PROPOSAL,
            "Client-side UUID content",
            0,
            Instant.now()
        );

        Task task = new Task(
            null, 1, "Client UUID Task", null, 1, null, null, null, null, null,
            List.of(), false, List.of(block), null, Instant.now(), Instant.now()
        );

        Task saved = repository.save(task);
        Task loaded = repository.findById(saved.id()).orElseThrow();

        assertEquals(clientSideId, loaded.discussion().get(0).id());
        assertEquals("Client-side UUID content", loaded.discussion().get(0).content());
    }

    @Test
    public void shouldUpdateFullTaskWithDiscussionAndSummary() {
        // 1. Create initial task
        Task task = repository.save(Task.create(1, "Initial Task", 1));
        
        // 2. Prepare updated task with discussion and summary
        DiscussionBlock block = createBlock("System", "Initial message", BlockType.MESSAGE, 0, null);
        Task updatedTask = new Task(
            task.id(),
            task.projectId(),
            "Updated Title",
            "New Description",
            task.creatorUserId(),
            task.assignedUserId(),
            task.statusId(),
            task.dueDate(),
            task.startDate(),
            task.estimatedHours(),
            List.of("new-tag"),
            false,
            List.of(block),
            "Updated Summary",
            task.createdAt(),
            Instant.now()
        );

        // 3. Save update
        repository.save(updatedTask);

        // 4. Verify
        Task loaded = repository.findById(task.id()).orElseThrow();
        assertEquals("Updated Title", loaded.title());
        assertEquals("New Description", loaded.description());
        assertEquals(1, loaded.discussion().size());
        assertEquals("Updated Summary", loaded.summary());
        assertTrue(loaded.tags().contains("new-tag"));
    }

    private DiscussionBlock createBlock(String author, String content, BlockType type, int level, UUID parentId) {
        return new DiscussionBlock(
            UUID.randomUUID(),
            parentId,
            author,
            type,
            content,
            level,
            Instant.now()
        );
    }

    private Task createTaskWithDiscussion(String author, String content, String title) {
        DiscussionBlock block = createBlock(author, content, BlockType.MESSAGE, 0, null);
        return new Task(
            null, 1, title, null, 1, null, null, null, null, null,
            List.of(), false, List.of(block), null, Instant.now(), Instant.now()
        );
    }
}
