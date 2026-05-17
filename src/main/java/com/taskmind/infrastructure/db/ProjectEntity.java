package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class ProjectEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "projectId")
    public Integer id;

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public Integer ownerUserId;

    public String color;
    public String icon;
    public boolean isActive;

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

    public Project toDomainModel() {
        return new Project(
            id,
            name,
            description,
            ownerUserId,
            color,
            icon,
            isActive,
            createdAt != null ? Instant.ofEpochMilli(createdAt) : null,
            updatedAt != null ? Instant.ofEpochMilli(updatedAt) : null
        );
    }

    public static ProjectEntity fromDomain(Project project) {
        var entity = new ProjectEntity();
        entity.id = project.id();
        entity.name = project.name();
        entity.description = project.description();
        entity.ownerUserId = project.ownerUserId();
        entity.color = project.color();
        entity.icon = project.icon();
        entity.isActive = project.isActive();
        entity.createdAt = project.createdAt() != null ? project.createdAt().toEpochMilli() : null;
        entity.updatedAt = project.updatedAt() != null ? project.updatedAt().toEpochMilli() : null;
        return entity;
    }
}
