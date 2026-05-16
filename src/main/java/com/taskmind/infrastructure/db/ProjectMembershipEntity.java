package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.model.ProjectMembership.MembershipRole;
import com.taskmind.domain.spi.MembershipRepository;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "project_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "projectId"})
})
public class ProjectMembershipEntity extends PanacheEntityBase {
    @Id public UUID id;
    public UUID userId;
    public UUID projectId;
    public String role;

    public static Optional<ProjectMembershipEntity> findByUserAndProject(UUID userId, UUID projectId) {
        return find("userId = ?1 and projectId = ?2", userId, projectId).firstResultOptional();
    }

    public static List<ProjectMembershipEntity> findByUser(UUID userId) {
        return list("userId", userId);
    }

    public static ProjectMembershipEntity fromDomain(ProjectMembership m) {
        var e = new ProjectMembershipEntity();
        e.id = UUID.randomUUID();
        e.userId = m.userId();
        e.projectId = m.projectId();
        e.role = m.role().name();
        return e;
    }
}
