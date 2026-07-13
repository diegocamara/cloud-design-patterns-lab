package org.example.integration.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PostgresClient {

  private final String jdbcUrl;
  private final String username;
  private final String password;

  PostgresClient(String jdbcUrl, String username, String password) {
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    this.username = Objects.requireNonNull(username, "username cannot be null");
    this.password = Objects.requireNonNull(password, "password cannot be null");
  }

  public Connection connection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, username, password);
  }

  public int update(String sql, Object... parameters) {
    try (var connection = connection();
        var statement = prepare(connection, sql, parameters)) {
      return statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not execute PostgreSQL update", exception);
    }
  }

  public long countRows(String tableName) {
    if (!isSafeIdentifier(tableName)) {
      throw new IllegalArgumentException("Invalid table name: " + tableName);
    }

    try (var connection = connection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("select count(*) from " + tableName)) {
      resultSet.next();
      return resultSet.getLong(1);
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not count rows from " + tableName, exception);
    }
  }

  public List<Map<String, Object>> queryForList(String sql, Object... parameters) {
    try (var connection = connection();
        var statement = prepare(connection, sql, parameters);
        var resultSet = statement.executeQuery()) {
      var metadata = resultSet.getMetaData();
      var columnCount = metadata.getColumnCount();
      var rows = new java.util.ArrayList<Map<String, Object>>();

      while (resultSet.next()) {
        var row = new java.util.LinkedHashMap<String, Object>();
        for (int column = 1; column <= columnCount; column++) {
          row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
        }
        rows.add(row);
      }

      return rows;
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not execute PostgreSQL query", exception);
    }
  }

  public void cleanApplicationTables() {
    try (var connection = connection();
        var statement = connection.createStatement()) {
      statement.execute(
          """
          truncate table completed_stages, outbox_events, players
          restart identity cascade
          """);
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not clean PostgreSQL application tables", exception);
    }
  }

  private PreparedStatement prepare(Connection connection, String sql, Object... parameters)
      throws SQLException {
    var statement = connection.prepareStatement(sql);
    for (int index = 0; index < parameters.length; index++) {
      statement.setObject(index + 1, parameters[index]);
    }
    return statement;
  }

  private boolean isSafeIdentifier(String value) {
    return value != null && value.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?");
  }
}
