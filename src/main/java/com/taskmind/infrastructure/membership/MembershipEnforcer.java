package com.taskmind.infrastructure.membership;

import com.taskmind.domain.spi.MembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MembershipEnforcer {
    @Inject MembershipRepository repo;

    public void assertAccess(Integer userId, Integer projectId) {
        if (repo.findByUserAndProject(userId, projectId).isEmpty()) {
            throw new SecurityException("User %s has no access to project %s".formatted(userId, projectId));
        }
    }

    // Role check logic might need roleId now. For now using findByUserAndProject.
    public void assertAdmin(Integer userId, Integer projectId) {
        var membership = repo.findByUserAndProject(userId, projectId)
                .orElseThrow(() -> new SecurityException("No membership found"));
        // Assuming roleId 1 is Admin based on schema-h2-v4.sql
        if (membership.roleId() != 1) {
            throw new SecurityException("User %s is not admin of project %s".formatted(userId, projectId));
        }
    }
}
