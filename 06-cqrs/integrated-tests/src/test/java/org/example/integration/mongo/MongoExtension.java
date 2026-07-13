package org.example.integration.mongo;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class MongoExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(MongoExtension.class);

  private static final String CLIENT_KEY = "mongo-client";

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(MongoClient.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return client(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    client(context).cleanApplicationCollections();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    client(context).cleanApplicationCollections();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    var client = context.getRoot().getStore(NAMESPACE).remove(CLIENT_KEY, MongoClient.class);
    if (client != null) {
      client.close();
    }
  }

  private MongoClient client(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    var client = store.get(CLIENT_KEY, MongoClient.class);

    if (client == null) {
      client = createClient();
      store.put(CLIENT_KEY, client);
    }

    return client;
  }

  private MongoClient createClient() {
    return new MongoClient(connectionString(), databaseName());
  }

  private String connectionString() {
    return propertyOrEnvironment(
        "integration.mongodb.uri",
        "INTEGRATION_MONGODB_URI",
        "mongodb://localhost:27017/progression_read_db?directConnection=true&uuidRepresentation=standard");
  }

  private String databaseName() {
    return propertyOrEnvironment(
        "integration.mongodb.database", "INTEGRATION_MONGODB_DATABASE", "progression_read_db");
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
