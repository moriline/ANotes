package com.taskmind.infrastructure.db;

import com.taskmind.domain.model.TimeEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.hibernate.query.NativeQuery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class H2TimeEntryRepository {

    /** Часы одного пользователя за период, сгруппированные по проекту. */
    public record ProjectTimeAggregate(Integer projectId, String projectName, long totalSeconds, int entryCount) {}

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

    /**
     * Списанное время пользователя в окне {@code [fromMillis, toMillis)} по
     * {@code startTime}, с разбивкой по проектам. {@code projectScope} ограничивает
     * выборку набором проектов (для отчёта, который смотрит коллега, а не сам
     * пользователь или админ); {@code null} — без ограничения.
     */
    public List<ProjectTimeAggregate> sumByUserGroupedByProject(
            Integer userId, long fromMillis, long toMillis, Set<Integer> projectScope) {

        if (projectScope != null && projectScope.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT t.projectId, p.name, SUM(te.seconds), COUNT(*)
            FROM timeEntries te
            JOIN tasks t ON t.taskId = te.taskId
            JOIN projects p ON p.projectId = t.projectId
            WHERE te.userId = :userId AND te.startTime >= :from AND te.startTime < :to
            """
            + (projectScope != null ? " AND t.projectId IN (:scope) " : "")
            + " GROUP BY t.projectId, p.name ORDER BY SUM(te.seconds) DESC";

        var query = TimeEntryEntity.getEntityManager().createNativeQuery(sql)
            .setParameter("userId", userId)
            .setParameter("from", fromMillis)
            .setParameter("to", toMillis);
        if (projectScope != null) {
            query.unwrap(NativeQuery.class).setParameterList("scope", projectScope);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
            .map(r -> new ProjectTimeAggregate(
                ((Number) r[0]).intValue(),
                (String) r[1],
                r[2] == null ? 0L : ((Number) r[2]).longValue(),
                ((Number) r[3]).intValue()))
            .toList();
    }
}
