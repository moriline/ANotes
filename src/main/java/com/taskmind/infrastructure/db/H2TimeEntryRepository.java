package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.TimeEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2TimeEntryRepository {

    @Transactional
    public TimeEntry save(TimeEntry entry) {
        var entity = TimeEntryEntity.fromDomain(entry);
        entity.persist();
        return entity.toDomainModel();
    }

    public List<TimeEntry> findByTaskId(Integer taskId) {
        return TimeEntryEntity.list("taskId", taskId).stream()
            .map(e -> ((TimeEntryEntity) e).toDomainModel())
            .collect(Collectors.toList());
    }

    public Long sumSecondsByTaskId(Integer taskId) {
        return (Long) TimeEntryEntity.getEntityManager()
            .createQuery("select sum(t.seconds) from TimeEntryEntity t where t.taskId = :taskId")
            .setParameter("taskId", taskId)
            .getSingleResult();
    }
}
