package com.pekara.controller.infrastructure.persistence;

import com.pekara.common.persistence.DatabaseClient;
import com.pekara.controller.domain.model.Execution;
import com.pekara.controller.domain.model.ExecutionStatus;
import com.pekara.controller.domain.model.StatusTransition;
import com.pekara.controller.domain.repository.ExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SqliteExecutionRepository implements ExecutionRepository {

    private final DatabaseClient databaseClient;

    public SqliteExecutionRepository(DatabaseClient databaseClient, String dbPath) {
        this.databaseClient = databaseClient;
        runMigrations(dbPath);
    }

    private void runMigrations(String dbPath) {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:sqlite:" + dbPath, null, null)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        log.info("Database migrations completed");
    }

    @Override
    public void save(Execution execution) {
        String sql = """
                INSERT INTO executions (id, script_name, script_content, required_cpus, current_status,
                                       exit_code, output, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        databaseClient.execute(sql,
                execution.getId(),
                execution.getScriptName(),
                execution.getScriptContent(),
                execution.getRequiredCpus(),
                execution.getCurrentStatus().name(),
                execution.getExitCode(),
                execution.getOutput(),
                execution.getCreatedAt().toString(),
                execution.getUpdatedAt().toString()
        );

        log.debug("Saved execution: {}", execution.getId());
    }

    @Override
    public void update(Execution execution) {
        String sql = """
                UPDATE executions
                SET script_name = ?, script_content = ?, required_cpus = ?, current_status = ?,
                    exit_code = ?, output = ?, updated_at = ?
                WHERE id = ?
                """;

        databaseClient.execute(sql,
                execution.getScriptName(),
                execution.getScriptContent(),
                execution.getRequiredCpus(),
                execution.getCurrentStatus().name(),
                execution.getExitCode(),
                execution.getOutput(),
                execution.getUpdatedAt().toString(),
                execution.getId()
        );

        log.debug("Updated execution: {}", execution.getId());
    }

    @Override
    public Optional<Execution> findById(String id) {
        String sql = """
                SELECT id, script_name, script_content, required_cpus, current_status,
                       exit_code, output, created_at, updated_at
                FROM executions
                WHERE id = ?
                """;

        List<Execution> results = databaseClient.query(sql, rs -> Execution.builder()
                .id(rs.getString("id"))
                .scriptName(rs.getString("script_name"))
                .scriptContent(rs.getString("script_content"))
                .requiredCpus(rs.getInt("required_cpus"))
                .currentStatus(ExecutionStatus.valueOf(rs.getString("current_status")))
                .exitCode((Integer) rs.getObject("exit_code"))
                .output(rs.getString("output"))
                .createdAt(Instant.parse(rs.getString("created_at")))
                .updatedAt(Instant.parse(rs.getString("updated_at")))
                .build(), id);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Execution> findAll() {
        String sql = """
                SELECT id, script_name, script_content, required_cpus, current_status,
                       exit_code, output, created_at, updated_at
                FROM executions
                ORDER BY created_at DESC
                """;

        return databaseClient.query(sql, rs -> Execution.builder()
                .id(rs.getString("id"))
                .scriptName(rs.getString("script_name"))
                .scriptContent(rs.getString("script_content"))
                .requiredCpus(rs.getInt("required_cpus"))
                .currentStatus(ExecutionStatus.valueOf(rs.getString("current_status")))
                .exitCode((Integer) rs.getObject("exit_code"))
                .output(rs.getString("output"))
                .createdAt(Instant.parse(rs.getString("created_at")))
                .updatedAt(Instant.parse(rs.getString("updated_at")))
                .build());
    }

    @Override
    public void addStatusTransition(String executionId, ExecutionStatus status, Instant timestamp) {
        String sql = """
                INSERT INTO status_history (execution_id, status, timestamp)
                VALUES (?, ?, ?)
                """;

        databaseClient.execute(sql, executionId, status.name(), timestamp.toString());
        log.debug("Added status transition for execution {}: {}", executionId, status);
    }

    @Override
    public List<StatusTransition> getStatusHistory(String executionId) {
        String sql = """
                SELECT id, execution_id, status, timestamp
                FROM status_history
                WHERE execution_id = ?
                ORDER BY timestamp ASC
                """;

        return databaseClient.query(sql, rs -> new StatusTransition(
                rs.getLong("id"),
                rs.getString("execution_id"),
                ExecutionStatus.valueOf(rs.getString("status")),
                Instant.parse(rs.getString("timestamp"))
        ), executionId);
    }
}
