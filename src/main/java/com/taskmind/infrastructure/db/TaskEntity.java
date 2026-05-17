package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Task;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "tasks")
public class TaskEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taskId")
    public Integer id;

    @Column(nullable = false)
    public Integer projectId;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false)
    public Integer creatorUserId;

    public Integer assignedUserId;
    public Integer statusId;

    public Long dueDate;
    public Long startDate;
    public Double estimatedHours;

    public String tags; // JSON Array as String

    public boolean isArchived;

    public Long createdAt;
    public Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().toEpochMilli();
    }

    public Task toDomainModel() {
        List<String> tagsList = tags == null || tags.isBlank() || tags.equals("[]") 
            ? List.of() 
            : Arrays.stream(tags.replace("[", "").replace("]", "").replace("\"", "").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

        return new Task(
            id,
            projectId,
            title,
            description,
            creatorUserId,
            assignedUserId,
            statusId,
            dueDate,
            startDate,
            estimatedHours,
            tagsList,
            isArchived,
            createdAt != null ? Instant.ofEpochMilli(createdAt) : null,
            updatedAt != null ? Instant.ofEpochMilli(updatedAt) : null
        );
    }

    public static TaskEntity fromDomain(Task t) {
        var e = new TaskEntity();
        e.id = t.id();
        e.projectId = t.projectId();
        e.title = t.title();
        e.description = t.description();
        e.creatorUserId = t.creatorUserId();
        e.assignedUserId = t.assignedUserId();
        e.statusId = t.statusId();
        e.dueDate = t.dueDate();
        e.startDate = t.startDate();
        e.estimatedHours = t.estimatedHours();
        e.tags = t.tags() == null || t.tags().isEmpty() 
            ? "[]" 
            : "[\"" + String.join("\",\"", t.tags()) + "\"]";
        e.isArchived = t.isArchived();
        e.createdAt = t.createdAt() != null ? t.createdAt().toEpochMilli() : null;
        e.updatedAt = t.updatedAt() != null ? t.updatedAt().toEpochMilli() : null;
        return e;
    }
}
