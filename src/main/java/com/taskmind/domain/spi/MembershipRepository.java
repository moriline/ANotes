package com.taskmind.domain.spi;

import com.taskmind.domain.model.ProjectMembership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    ProjectMembership save(ProjectMembership membership);
    Optional<ProjectMembership> findByUserAndProject(Integer userId, Integer projectId);
    List<ProjectMembership> findByUser(Integer userId);
    List<ProjectMembership> findByProject(Integer projectId);
    void deleteById(Integer id);
}
