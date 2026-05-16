package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(unique = true, nullable = false) public String username;
    @Column(unique = true, nullable = false) public String email;
    public String passwordHash;
    public String rolesCsv;
    public Instant createdAt;

    public User toDomainModel() {
        Set<User.Role> roles = rolesCsv == null ? Set.of()
            : Set.of(rolesCsv.split(",")).stream().map(User.Role::valueOf).collect(Collectors.toSet());
        return new User(id, username, email, passwordHash, roles, createdAt);
    }

    public static UserEntity fromDomain(User u) {
        var e = new UserEntity();
        e.id = u.id(); e.username = u.username(); e.email = u.email();
        e.passwordHash = u.passwordHash();
        e.rolesCsv = String.join(",", u.roles().stream().map(Enum::name).toList());
        e.createdAt = u.createdAt();
        return e;
    }
}
