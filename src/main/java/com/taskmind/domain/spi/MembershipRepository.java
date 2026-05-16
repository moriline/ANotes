package com.taskmind.domain.spi;

import com.taskmind.domain.model.ProjectMembership;
import java.util.List;
import java.util.UUID;

public interface MembershipRepository {
    void save(ProjectMembership membership);
    boolean isOwner(UUID userId, UUID projectId);
    boolean hasAccess(UUID userId, UUID projectId);
    List<ProjectMembership> findByUser(UUID userId);
}
