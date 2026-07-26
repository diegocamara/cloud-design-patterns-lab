package org.example.idempotency.environment;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgreSQLExtension
    implements BeforeAllCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

  private static final String DATABASE_NAME = "idempotency_db";
  private static final String USERNAME = "idempotency_user";
  private static final String PASSWORD = "idempotency_pass";

  private final PostgreSQLContainer container =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
          .withDatabaseName(DATABASE_NAME)
          .withUsername(USERNAME)
          .withPassword(PASSWORD);

  private PostgresClient client;

  @Override
  public void beforeAll(ExtensionContext context) {
    this.container.start();
    this.client = new PostgresClient(jdbcUrl(), username(), password());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    truncateAllTables();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    this.container.stop();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(PostgresClient.class);
  }

  @Override
  public PostgresClient resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return client();
  }

  public void truncateAllTables() {
    client().cleanApplicationTables();
  }

  public String jdbcUrl() {
    return this.container.getJdbcUrl();
  }

  public String username() {
    return this.container.getUsername();
  }

  public String password() {
    return this.container.getPassword();
  }

  private PostgresClient client() {
    if (this.client == null) {
      throw new IllegalStateException("PostgreSQL client is not available before the container starts");
    }

    return this.client;
  }
}
