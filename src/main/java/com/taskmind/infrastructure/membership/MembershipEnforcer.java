package com.taskmind.infrastructure.membership;

import com.taskmind.domain.spi.MembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class MembershipEnforcer {
    @Inject MembershipRepository repo;

    public void assertAccess(UUID userId, UUID projectId) {
        if (!repo.hasAccess(userId, projectId)) {
            throw new SecurityException("User %s has no access to project %s".formatted(userId, projectId));
        }
    }

    public void assertOwner(UUID userId, UUID projectId) {
        if (!repo.isOwner(userId, projectId)) {
            throw new SecurityException("User %s is not owner of project %s".formatted(userId, projectId));
        }
    }
}
