package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Task;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskEntity extends PanacheEntityBase {

    @Id
    public UUID id;
    public UUID projectId;
    public String title;
    @Column(length = 4000)
    public String description;
    public String tagsJson;
    @Enumerated(EnumType.STRING)
    public Task.Status status;
    public Instant createdAt;

    public Task toDomainModel() {
        var tags = tagsJson == null || tagsJson.isBlank() ? List.<String>of()
                   : List.of(tagsJson.split(","));
        return new Task(id, projectId, title, description, tags, status, createdAt);
    }

    public static TaskEntity fromDomain(Task t) {
        var e = new TaskEntity();
        e.id = t.id();
        e.projectId = t.projectId();
        e.title = t.title();
        e.description = t.description();
        e.tagsJson = String.join(",", t.tags());
        e.status = t.status();
        e.createdAt = t.createdAt();
        return e;
    }
}
