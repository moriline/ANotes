package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2ProjectRepository implements ProjectRepository {

    @Override
    @Transactional
    public Project save(Project project) {
        var entity = ProjectEntity.fromDomain(project);
        if (entity.id == null) {
            entity.persist();
        } else {
            entity = entity.getEntityManager().merge(entity);
        }
        return entity.toDomainModel();
    }

    @Override
    public Optional<Project> findById(Integer id) {
        return ProjectEntity.<ProjectEntity>findByIdOptional(id)
            .map(ProjectEntity::toDomainModel);
    }

    @Override
    public Optional<Project> findByName(String name) {
        return ProjectEntity.<ProjectEntity>find("name", name).firstResultOptional()
            .map(ProjectEntity::toDomainModel);
    }

    @Override
    public List<Project> findAllActive() {
        return ProjectEntity.<ProjectEntity>list("isActive", true)
            .stream().map(ProjectEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public List<Project> findByOwner(Integer ownerUserId) {
        return ProjectEntity.<ProjectEntity>list("ownerUserId", ownerUserId)
            .stream().map(ProjectEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        ProjectEntity.deleteById(id);
    }
}
