package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Action;
import com.taskmind.domain.model.ProjectRole;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "projectRoles")
public class ProjectRoleEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleId")
    public Integer id;

    @Column(nullable = false, unique = true)
    public String roleName;

    public String description;

    @Convert(converter = ActionSetConverter.class)
    public Set<Action> permissions;

    public ProjectRole toDomainModel() {
        return new ProjectRole(id, roleName, description, permissions);
    }

    public static ProjectRoleEntity fromDomain(ProjectRole r) {
        var e = new ProjectRoleEntity();
        e.id = r.id();
        e.roleName = r.name();
        e.description = r.description();
        e.permissions = r.permissions();
        return e;
    }
}
