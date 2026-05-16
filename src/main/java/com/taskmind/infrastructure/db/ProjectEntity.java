package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(unique = true, nullable = false)
    public String name;

    public String rootPath;
    public Instant createdAt;

    @Enumerated(EnumType.STRING)
    public Project.Status status;

    public Project toDomainModel() {
        return new Project(id, name, rootPath, createdAt, status);
    }

    public static ProjectEntity fromDomain(Project project) {
        var entity = new ProjectEntity();
        entity.id = project.id();
        entity.name = project.name();
        entity.rootPath = project.rootPath();
        entity.createdAt = project.createdAt();
        entity.status = project.status();
        return entity;
    }
}
