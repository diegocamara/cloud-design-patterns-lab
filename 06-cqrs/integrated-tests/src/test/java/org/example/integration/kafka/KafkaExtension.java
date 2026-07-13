package org.example.integration.kafka;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class KafkaExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(KafkaExtension.class);

  private static final String CLIENT_KEY = "kafka-client";

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(KafkaClient.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return client(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    client(context).cleanApplicationTopics();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    client(context).cleanApplicationTopics();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    var client = context.getRoot().getStore(NAMESPACE).remove(CLIENT_KEY, KafkaClient.class);
    if (client != null) {
      client.close();
    }
  }

  private KafkaClient client(ExtensionContext context) {
    var store = context.getRoot().getStore(NAMESPACE);
    var client = store.get(CLIENT_KEY, KafkaClient.class);

    if (client == null) {
      client = createClient();
      store.put(CLIENT_KEY, client);
    }

    return client;
  }

  private KafkaClient createClient() {
    return new KafkaClient(bootstrapServers(), topic());
  }

  private String bootstrapServers() {
    return propertyOrEnvironment(
        "integration.kafka.bootstrap-servers",
        "INTEGRATION_KAFKA_BOOTSTRAP_SERVERS",
        "localhost:29092");
  }

  private String topic() {
    return propertyOrEnvironment(
        "integration.kafka.topic", "INTEGRATION_KAFKA_TOPIC", "game.progression.events");
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
