package org.example.integration.environment;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class DockerComposeEnvironmentExtension implements BeforeAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(DockerComposeEnvironmentExtension.class);

  private static final String RESOURCE_KEY = "docker-compose-environment";
  private static final Logger LOGGER =
      System.getLogger(DockerComposeEnvironmentExtension.class.getName());

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(NAMESPACE)
        .computeIfAbsent(
            RESOURCE_KEY, ignored -> new EnvironmentResource(), EnvironmentResource.class);
  }

  private static final class EnvironmentResource implements AutoCloseable {

    private static final int WRITE_PORT = 8080;
    private static final int READER_PORT = 8081;
    private static final int POSTGRES_PORT = 5432;
    private static final int MONGO_PORT = 27017;

    private final Map<String, String> previousProperties = new LinkedHashMap<>();
    private final ComposeContainer environment;

    private EnvironmentResource() {
      this.environment = createEnvironment();
      LOGGER.log(Level.INFO, "Starting Docker Compose integration environment...");
      this.environment.start();
      publishConnectionProperties();
      LOGGER.log(Level.INFO, "Docker Compose integration environment is running.");
    }

    @Override
    public void close() {
      LOGGER.log(Level.INFO, "Stopping Docker Compose integration environment...");
      restoreConnectionProperties();
      environment.stop();
      LOGGER.log(Level.INFO, "Docker Compose integration environment stopped.");
    }

    private ComposeContainer createEnvironment() {
      return new ComposeContainer(DockerImageName.parse("docker"), composeFile())
          .withBuild(true)
          .withExposedService(
              "postgres-write-1",
              POSTGRES_PORT,
              Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
          .withExposedService(
              "mongo-read-1",
              MONGO_PORT,
              Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
          .withExposedService(
              "game-progression-write-1",
              WRITE_PORT,
              Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
          .withExposedService(
              "game-progression-reader-1",
              READER_PORT,
              Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));
    }

    private File composeFile() {
      var userDirectory = new File(System.getProperty("user.dir"));
      var projectDirectory =
          "integrated-tests".equals(userDirectory.getName())
              ? userDirectory.getParentFile()
              : userDirectory;

      return new File(projectDirectory, "docker-compose.integrated-tests.yml");
    }

    private void publishConnectionProperties() {
      setProperty(
          "integration.write.base-url",
          "http://"
              + environment.getServiceHost("game-progression-write-1", WRITE_PORT)
              + ":"
              + environment.getServicePort("game-progression-write-1", WRITE_PORT));

      setProperty(
          "integration.reader.base-url",
          "http://"
              + environment.getServiceHost("game-progression-reader-1", READER_PORT)
              + ":"
              + environment.getServicePort("game-progression-reader-1", READER_PORT));

      setProperty(
          "integration.postgres.jdbc-url",
          "jdbc:postgresql://"
              + environment.getServiceHost("postgres-write-1", POSTGRES_PORT)
              + ":"
              + environment.getServicePort("postgres-write-1", POSTGRES_PORT)
              + "/game_progression_write_db");

      setProperty(
          "integration.mongodb.uri",
          "mongodb://"
              + environment.getServiceHost("mongo-read-1", MONGO_PORT)
              + ":"
              + environment.getServicePort("mongo-read-1", MONGO_PORT)
              + "/progression_read_db?directConnection=true&uuidRepresentation=standard");

      setProperty("integration.kafka.bootstrap-servers", "localhost:29092");

      LOGGER.log(
          Level.INFO,
          "Integration write API: {0}",
          System.getProperty("integration.write.base-url"));
      LOGGER.log(
          Level.INFO,
          "Integration reader API: {0}",
          System.getProperty("integration.reader.base-url"));
      LOGGER.log(
          Level.INFO,
          "Integration PostgreSQL JDBC URL: {0}",
          System.getProperty("integration.postgres.jdbc-url"));
      LOGGER.log(
          Level.INFO,
          "Integration MongoDB URI: {0}",
          System.getProperty("integration.mongodb.uri"));
      LOGGER.log(
          Level.INFO,
          "Integration Kafka bootstrap servers: {0}",
          System.getProperty("integration.kafka.bootstrap-servers"));
    }

    private void setProperty(String name, String value) {
      previousProperties.put(name, System.getProperty(name));
      System.setProperty(name, value);
    }

    private void restoreConnectionProperties() {
      previousProperties.forEach(
          (name, value) -> {
            if (value == null) {
              System.clearProperty(name);
            } else {
              System.setProperty(name, value);
            }
          });
    }
  }
}
