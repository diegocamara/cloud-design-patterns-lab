package org.example.orders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PostgreSQLClientExtension implements AfterEachCallback {

  private static final String FIND_TABLES_SQL =
      """
      SELECT schemaname, tablename
        FROM pg_catalog.pg_tables
       WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
         AND tableowner = current_user
         AND tablename <> 'flyway_schema_history'
       ORDER BY schemaname, tablename
      """;

  private final Supplier<String> jdbcUrlSupplier;
  private final String username;
  private final String password;

  public PostgreSQLClientExtension(String jdbcUrl, String username, String password) {
    this(() -> jdbcUrl, username, password);
  }

  public PostgreSQLClientExtension(
      Supplier<String> jdbcUrlSupplier, String username, String password) {
    this.jdbcUrlSupplier =
        Objects.requireNonNull(jdbcUrlSupplier, "jdbcUrlSupplier must not be null");
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
  }

  @Override
  public void afterEach(ExtensionContext context) {
    truncateAllTables();
  }

  public void truncateAllTables() {
    String jdbcUrl = Objects.requireNonNull(this.jdbcUrlSupplier.get(), "jdbcUrl must not be null");

    try (Connection connection = DriverManager.getConnection(jdbcUrl, this.username, this.password)) {
      List<String> tables = findTables(connection);

      if (!tables.isEmpty()) {
        truncateTables(connection, tables);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not clean PostgreSQL database: " + jdbcUrl, exception);
    }
  }

  public long countRows(String tableName) {
    String jdbcUrl = Objects.requireNonNull(this.jdbcUrlSupplier.get(), "jdbcUrl must not be null");
    String sql = "SELECT COUNT(*) FROM " + quoteIdentifier(tableName);

    try (Connection connection = DriverManager.getConnection(jdbcUrl, this.username, this.password);
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getLong(1);
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Could not count rows from PostgreSQL table " + tableName, exception);
    }
  }

  public boolean isAvailable() {
    String jdbcUrl = Objects.requireNonNull(this.jdbcUrlSupplier.get(), "jdbcUrl must not be null");

    try (Connection connection = DriverManager.getConnection(jdbcUrl, this.username, this.password);
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery("SELECT 1")) {
      return resultSet.next() && resultSet.getInt(1) == 1;
    } catch (SQLException exception) {
      return false;
    }
  }

  private static List<String> findTables(Connection connection) throws SQLException {
    List<String> tables = new ArrayList<>();

    try (var statement = connection.prepareStatement(FIND_TABLES_SQL);
        var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        tables.add(
            quoteIdentifier(resultSet.getString("schemaname"))
                + "."
                + quoteIdentifier(resultSet.getString("tablename")));
      }
    }

    return tables;
  }

  private static void truncateTables(Connection connection, List<String> tables)
      throws SQLException {
    String sql = "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE";

    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static String quoteIdentifier(String identifier) {
    return '"' + identifier.replace("\"", "\"\"") + '"';
  }
}
