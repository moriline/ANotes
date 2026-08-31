package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    public Integer id;

    @Column(unique = true, nullable = false)
    public String username;

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String password;

    public String displayName;
    public String avatarUrl;
    public boolean isActive;

    /** Глобальный администратор: см. com.taskmind.domain.model.User#isAdmin. */
    public boolean isAdmin;

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

    public User toDomainModel() {
        return new User(
            id,
            username,
            email,
            password,
            displayName,
            avatarUrl,
            isActive,
            isAdmin,
            createdAt != null ? Instant.ofEpochMilli(createdAt) : null,
            updatedAt != null ? Instant.ofEpochMilli(updatedAt) : null
        );
    }

    public static UserEntity fromDomain(User u) {
        var e = new UserEntity();
        e.id = u.id();
        e.username = u.username();
        e.email = u.email();
        e.password = u.password();
        e.displayName = u.displayName();
        e.avatarUrl = u.avatarUrl();
        e.isActive = u.isActive();
        e.isAdmin = u.isAdmin();
        e.createdAt = u.createdAt() != null ? u.createdAt().toEpochMilli() : null;
        e.updatedAt = u.updatedAt() != null ? u.updatedAt().toEpochMilli() : null;
        return e;
    }
}
