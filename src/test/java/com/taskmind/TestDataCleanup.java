package com.taskmind;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Возвращает тестовую базу к состоянию сразу после инициализации: чистит таблицы
 * данных, перезапускает счётчики identity и заново проигрывает seed-h2-v4.sql.
 *
 * <p>Раньше метод просто удалял users/projects/tasks и ничего не восстанавливал.
 * База в тестах одна на весь прогон, поэтому классы, зависящие от сида
 * (admin с userId=1, projectId=1, taskId=1), падали или проходили в зависимости
 * от того, в каком порядке surefire запустил классы. Теперь любой класс,
 * вызвавший {@code clearAll()}, получает один и тот же набор данных с теми же
 * идентификаторами, независимо от соседей по прогону.
 *
 * <p>{@code projectRoles} намеренно не трогаем: это справочник, который создаёт
 * DDL-скрипт, и тесты завязаны на фиксированные roleId 1..6.
 */
@ApplicationScoped
public class TestDataCleanup {

    private record Table(String name, String primaryKey) {}

    /** Дочерние таблицы идут первыми — порядок важен, если FK вдруг окажутся включены. */
    private static final List<Table> DATA_TABLES = List.of(
        new Table("activityLog", "activityId"),
        new Table("timeEntries", "entryId"),
        new Table("comments", "commentId"),
        new Table("files", "fileId"),
        new Table("tasks", "taskId"),
        new Table("projectStatuses", "statusId"),
        new Table("projectMembers", "projectMemberId"),
        new Table("projects", "projectId"),
        new Table("users", "userId")
    );

    @Inject DataSource dataSource;

    public void clearAll() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (Table table : DATA_TABLES) {
                    statement.execute("DELETE FROM " + table.name());
                    statement.execute("ALTER TABLE " + table.name()
                        + " ALTER COLUMN " + table.primaryKey() + " RESTART WITH 1");
                }
                statement.execute("RUNSCRIPT FROM 'classpath:seed-h2-v4.sql'");
            } finally {
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сбросить тестовые данные", e);
        }
    }
}
