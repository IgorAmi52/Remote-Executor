package com.pekara.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public interface DatabaseClient {

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);

    int execute(String sql, Object... params);

    void executeInTransaction(Consumer<Connection> operation);

    Connection getConnection() throws SQLException;

    void close();
}
