package com.taskmind.domain.spi;

import com.taskmind.domain.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(Integer id);
    Optional<Project> findByName(String name);
    List<Project> findAllActive();
    void deleteById(Integer id);
}
