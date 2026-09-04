package com.taskmind.infrastructure.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class CommentEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentId")
    public Integer id;

    @Column(nullable = false)
    public Integer taskId;

    @Column(nullable = false)
    public Integer userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String content;

    /** PUBLIC | INTERNAL | SYSTEM. Хранится строкой; см. {@link com.taskmind.domain.model.Visibility}. */
    @Column(nullable = false)
    public String visibility = "PUBLIC";

    public Long createdAt;
    public Boolean isEdited = false;
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
        isEdited = true;
    }
}
