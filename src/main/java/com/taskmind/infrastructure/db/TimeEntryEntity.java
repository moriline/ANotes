package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.TimeEntry;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "timeEntries")
public class TimeEntryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entryId")
    public Integer id;

    @Column(nullable = false)
    public Integer taskId;

    @Column(nullable = false)
    public Integer userId;

    @Column(nullable = false)
    public Long seconds;

    public String description;

    @Column(nullable = false)
    public Long startTime;

    public Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now().toEpochMilli();
    }

    public TimeEntry toDomainModel() {
        return new TimeEntry(
            id,
            taskId,
            userId,
            seconds,
            description,
            startTime != null ? Instant.ofEpochMilli(startTime) : null,
            createdAt != null ? Instant.ofEpochMilli(createdAt) : null
        );
    }

    public static TimeEntryEntity fromDomain(TimeEntry entry) {
        var entity = new TimeEntryEntity();
        entity.id = entry.id();
        entity.taskId = entry.taskId();
        entity.userId = entry.userId();
        entity.seconds = entry.seconds();
        entity.description = entry.description();
        entity.startTime = entry.startTime() != null ? entry.startTime().toEpochMilli() : null;
        entity.createdAt = entry.createdAt() != null ? entry.createdAt().toEpochMilli() : null;
        return entity;
    }
}
