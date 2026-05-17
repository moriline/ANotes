package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.ProjectMembership;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "projectMembers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projectId", "userId"})
})
public class ProjectMembershipEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "projectMemberId")
    public Integer id;

    @Column(nullable = false)
    public Integer projectId;

    @Column(nullable = false)
    public Integer userId;

    @Column(nullable = false)
    public Integer roleId;

    public Long joinedAt;

    public ProjectMembership toDomainModel() {
        return new ProjectMembership(id, projectId, userId, roleId, joinedAt);
    }

    public static ProjectMembershipEntity fromDomain(ProjectMembership m) {
        var e = new ProjectMembershipEntity();
        e.id = m.id();
        e.projectId = m.projectId();
        e.userId = m.userId();
        e.roleId = m.roleId();
        e.joinedAt = m.joinedAt();
        return e;
    }
}
