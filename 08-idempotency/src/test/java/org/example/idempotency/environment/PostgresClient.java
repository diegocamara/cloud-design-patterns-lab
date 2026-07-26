package org.example.idempotency.environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PostgresClient {

  private static final String FIND_APPLICATION_TABLES_SQL =
      """
      SELECT schemaname, tablename
        FROM pg_catalog.pg_tables
       WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
         AND tableowner = current_user
         AND tablename <> 'flyway_schema_history'
       ORDER BY schemaname, tablename
      """;

  private final String jdbcUrl;
  private final String username;
  private final String password;

  PostgresClient(String jdbcUrl, String username, String password) {
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
  }

  public int update(String sql, Object... parameters) {
    try (var connection = connection();
        var statement = prepare(connection, sql, parameters)) {
      return statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not execute PostgreSQL update", exception);
    }
  }

  public List<Map<String, Object>> queryForList(String sql, Object... parameters) {
    try (var connection = connection();
        var statement = prepare(connection, sql, parameters);
        var resultSet = statement.executeQuery()) {
      var metadata = resultSet.getMetaData();
      var rows = new ArrayList<Map<String, Object>>();

      while (resultSet.next()) {
        var row = new LinkedHashMap<String, Object>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
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
      var tables = applicationTables(connection);

      if (!tables.isEmpty()) {
        statement.execute(
            "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not clean PostgreSQL application tables", exception);
    }
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(this.jdbcUrl, this.username, this.password);
  }

  private PreparedStatement prepare(Connection connection, String sql, Object... parameters)
      throws SQLException {
    var statement = connection.prepareStatement(sql);

    for (int index = 0; index < parameters.length; index++) {
      statement.setObject(index + 1, parameters[index]);
    }

    return statement;
  }

  private List<String> applicationTables(Connection connection) throws SQLException {
    try (var statement = connection.prepareStatement(FIND_APPLICATION_TABLES_SQL);
        var resultSet = statement.executeQuery()) {
      var tables = new ArrayList<String>();

      while (resultSet.next()) {
        tables.add(
            quoteIdentifier(resultSet.getString("schemaname"))
                + "."
                + quoteIdentifier(resultSet.getString("tablename")));
      }

      return tables;
    }
  }

  private String quoteIdentifier(String identifier) {
    return '"' + identifier.replace("\"", "\"\"") + '"';
  }
}
