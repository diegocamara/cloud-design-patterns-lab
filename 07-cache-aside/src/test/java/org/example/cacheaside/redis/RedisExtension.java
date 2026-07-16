package org.example.cacheaside.redis;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class RedisExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(RedisExtension.class);

  private static final String CLIENT_KEY = "redis-client";

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(RedisClient.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return client(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    client(context).cleanApplicationKeys();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    client(context).cleanApplicationKeys();
  }

  private RedisClient client(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    var client = store.get(CLIENT_KEY, RedisClient.class);

    if (client == null) {
      client = createClient();
      store.put(CLIENT_KEY, client);
    }

    return client;
  }

  private RedisClient createClient() {
    return new RedisClient(host(), port());
  }

  private String host() {
    return propertyOrEnvironment("integration.redis.host", "INTEGRATION_REDIS_HOST", "localhost");
  }

  private int port() {
    return Integer.parseInt(
        propertyOrEnvironment("integration.redis.port", "INTEGRATION_REDIS_PORT", "6379"));
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
