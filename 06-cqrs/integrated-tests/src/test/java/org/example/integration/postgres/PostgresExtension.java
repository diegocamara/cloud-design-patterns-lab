package org.example.integration.postgres;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class PostgresExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(PostgresExtension.class);

  private static final String CLIENT_KEY = "postgres-client";

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(PostgresClient.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return client(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    client(context).cleanApplicationTables();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    client(context).cleanApplicationTables();
  }

  private PostgresClient client(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    var client = store.get(CLIENT_KEY, PostgresClient.class);

    if (client == null) {
      client = createClient();
      store.put(CLIENT_KEY, client);
    }

    return client;
  }

  private PostgresClient createClient() {
    return new PostgresClient(jdbcUrl(), username(), password());
  }

  private String jdbcUrl() {
    return propertyOrEnvironment(
        "integration.postgres.jdbc-url",
        "INTEGRATION_POSTGRES_JDBC_URL",
        "jdbc:postgresql://localhost:5432/game_progression_write_db");
  }

  private String username() {
    return propertyOrEnvironment(
        "integration.postgres.username",
        "INTEGRATION_POSTGRES_USERNAME",
        "game_progression_write_user");
  }

  private String password() {
    return propertyOrEnvironment(
        "integration.postgres.password",
        "INTEGRATION_POSTGRES_PASSWORD",
        "game_progression_write_pass");
  }

  private String propertyOrEnvironment(String propertyName, String environmentName, String fallback) {
    var propertyValue = System.getProperty(propertyName);
    if (propertyValue != null && !propertyValue.isBlank()) {
      return propertyValue;
    }

    var environmentValue = System.getenv(environmentName);
    if (environmentValue != null && !environmentValue.isBlank()) {
      return environmentValue;
    }

    return fallback;
  }
}
