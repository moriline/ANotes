package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.model.ProjectMembership.MembershipRole;
import com.taskmind.domain.spi.MembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2MembershipRepository implements MembershipRepository {
    @Override @Transactional
    public void save(ProjectMembership membership) {
        var entity = ProjectMembershipEntity.fromDomain(membership);
        entity.persist();
    }

    @Override
    public boolean isOwner(UUID userId, UUID projectId) {
        return ProjectMembershipEntity.findByUserAndProject(userId, projectId)
            .map(e -> MembershipRole.OWNER.name().equals(e.role))
            .orElse(false);
    }

    @Override
    public boolean hasAccess(UUID userId, UUID projectId) {
        return ProjectMembershipEntity.findByUserAndProject(userId, projectId).isPresent();
    }

    @Override
    public List<ProjectMembership> findByUser(UUID userId) {
        return ProjectMembershipEntity.findByUser(userId).stream()
            .map(e -> new ProjectMembership(e.userId, e.projectId, MembershipRole.valueOf(e.role)))
            .collect(Collectors.toList());
    }
}
