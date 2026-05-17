package com.taskmind.infrastructure.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "files")
public class FileEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fileId")
    public Integer id;

    @Column(nullable = false)
    public Integer taskId;

    @Column(nullable = false)
    public Integer projectId;

    @Column(nullable = false)
    public String fileName;

    @Column(nullable = false)
    public String fileOriginalName;

    @Column(nullable = false)
    public Long fileSize;

    @Column(nullable = false)
    public String mimeType;

    @Column(nullable = false)
    public String fileUrl;

    @Column(nullable = false)
    public Integer uploadedByUserId;

    public String checksum;
    public Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now().toEpochMilli();
    }
}
