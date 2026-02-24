package com.pekara.common.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SqliteClient implements DatabaseClient {

    private final HikariDataSource dataSource;

    public SqliteClient(String dbPath) {
        this(dbPath, 5); // Default pool size of 5
    }

    public SqliteClient(String dbPath, int maxPoolSize) {
        ensureParentDirectoryExists(dbPath);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // SQLite specific settings
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);
        log.info("SQLite connection pool initialized for database: {}", dbPath);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }

            log.debug("Query executed successfully, returned {} rows", results.size());
            return results;

        } catch (SQLException e) {
            log.error("Error executing query: {}", sql, e);
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Override
    public int execute(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);
            int affectedRows = stmt.executeUpdate();

            log.debug("Execute completed, {} rows affected", affectedRows);
            return affectedRows;

        } catch (SQLException e) {
            log.error("Error executing statement: {}", sql, e);
            throw new RuntimeException("Database execute failed", e);
        }
    }

    @Override
    public void executeInTransaction(Consumer<Connection> operation) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            operation.accept(conn);

            conn.commit();
            log.debug("Transaction committed successfully");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warn("Transaction rolled back due to error", e);
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw new RuntimeException("Transaction failed", e);

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error closing connection", e);
                }
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("SQLite connection pool closed");
        }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private void ensureParentDirectoryExists(String dbPath) {
        Path dbFilePath = Paths.get(dbPath).toAbsolutePath().normalize();
        Path parentDir = dbFilePath.getParent();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir);
                log.info("Created/verified database directory: {}", parentDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create database directory: " + parentDir, e);
            }
        }
    }
}
