package com.taskmind.infrastructure.db;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.h2.tools.RunScript;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Разово создаёт схему (и через неё — сид) на пустой базе при старте приложения.
 *
 * <p>Раньше это делал сам JDBC-урл через {@code INIT=RUNSCRIPT FROM 'classpath:schema-h2-v4.sql'}.
 * Проблема: H2 выполняет {@code INIT} при каждом новом физическом открытии файла, а не
 * только при первом создании. Для {@code anotes.mv.db} (файл коммитится в репозиторий
 * уже с готовой схемой и сидом) это означало, что любой холодный старт нового JVM —
 * обычный {@code mvn quarkus:dev} после перезапуска, не только hot-reload — валился на
 * первом же запросе к БД с {@code Table "USERS" already exists}: CREATE TABLE в
 * schema-h2-v4.sql не идемпотентны. Для {@code jdbc:h2:mem:anotes-test} с этим
 * никогда не сталкивались, потому что in-memory база каждый раз действительно пустая.
 *
 * <p>Здесь тот же schema-h2-v4.sql выполняется вручную и только если таблиц ещё нет —
 * содержимое файла (включая его собственный {@code RUNSCRIPT FROM 'classpath:seed-h2-v4.sql'}
 * в конце) не меняется, меняется только условие запуска.
 *
 * <p>Существование схемы проверяется прямым запросом к {@code PUBLIC.users}, а не через
 * {@code DatabaseMetaData.getTables(null, null, "USERS", null)}: без указания схемы это
 * находит системную {@code INFORMATION_SCHEMA.USERS} (список пользователей самой H2) и
 * ложно считает схему уже существующей на пустой базе.
 */
@ApplicationScoped
public class DatabaseSchemaInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseSchemaInitializer.class);

    @Inject
    DataSource dataSource;

    void onStart(@Observes StartupEvent event) {
        try (Connection connection = dataSource.getConnection()) {
            if (schemaAlreadyExists(connection)) {
                return;
            }
            LOG.info("Таблица users не найдена — инициализирую схему из schema-h2-v4.sql");
            try (InputStream in = getClass().getResourceAsStream("/schema-h2-v4.sql")) {
                if (in == null) {
                    throw new IllegalStateException("schema-h2-v4.sql не найден в classpath");
                }
                RunScript.execute(connection, new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Не удалось инициализировать схему БД", e);
        }
    }

    private boolean schemaAlreadyExists(Connection connection) {
        try (var statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM PUBLIC.users FETCH FIRST 1 ROWS ONLY").close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
