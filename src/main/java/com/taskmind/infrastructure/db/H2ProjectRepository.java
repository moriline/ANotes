package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.Project;
import com.taskmind.domain.spi.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2ProjectRepository implements ProjectRepository {

    @Override
    @Transactional
    public Project save(Project project) {
        var entity = ProjectEntity.fromDomain(project);
        entity.persist();
        return entity.toDomainModel();
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return ProjectEntity.<ProjectEntity>find("id", id).firstResultOptional()
            .map(ProjectEntity::toDomainModel);
    }

    @Override
    public Optional<Project> findByName(String name) {
        return ProjectEntity.<ProjectEntity>find("name", name).firstResultOptional()
            .map(ProjectEntity::toDomainModel);
    }

    @Override
    public List<Project> findAllActive() {
        return ProjectEntity.<ProjectEntity>list("status", Project.Status.ACTIVE)
            .stream().map(ProjectEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        ProjectEntity.deleteById(id);
    }
}
