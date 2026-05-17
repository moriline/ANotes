package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.ProjectMembership;
import com.taskmind.domain.spi.MembershipRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2MembershipRepository implements MembershipRepository {

    @Override
    @Transactional
    public ProjectMembership save(ProjectMembership membership) {
        var entity = ProjectMembershipEntity.fromDomain(membership);
        if (entity.id == null) {
            entity.persist();
        } else {
            entity = entity.getEntityManager().merge(entity);
        }
        return entity.toDomainModel();
    }

    @Override
    public Optional<ProjectMembership> findByUserAndProject(Integer userId, Integer projectId) {
        return ProjectMembershipEntity.find("userId = ?1 and projectId = ?2", userId, projectId)
                .firstResultOptional()
                .map(e -> ((ProjectMembershipEntity) e).toDomainModel());
    }

    @Override
    public List<ProjectMembership> findByUser(Integer userId) {
        return ProjectMembershipEntity.list("userId", userId)
                .stream()
                .map(e -> ((ProjectMembershipEntity) e).toDomainModel())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        ProjectMembershipEntity.deleteById(id);
    }
}
