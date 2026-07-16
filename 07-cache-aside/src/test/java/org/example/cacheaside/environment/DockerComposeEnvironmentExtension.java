package org.example.cacheaside.environment;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class DockerComposeEnvironmentExtension implements BeforeAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(DockerComposeEnvironmentExtension.class);

  private static final String RESOURCE_KEY = "docker-compose-environment";
  private static EnvironmentResource environment;

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(NAMESPACE)
        .computeIfAbsent(RESOURCE_KEY, ignored -> environment(), EnvironmentResource.class);
  }

  public static synchronized String postgresJdbcUrl() {
    return environment().postgresJdbcUrl();
  }

  public static synchronized String postgresUsername() {
    return environment().postgresUsername();
  }

  public static synchronized String postgresPassword() {
    return environment().postgresPassword();
  }

  public static synchronized String redisHost() {
    return environment().redisHost();
  }

  public static synchronized int redisPort() {
    return environment().redisPort();
  }

  private static synchronized EnvironmentResource environment() {
    if (environment == null) {
      environment = new EnvironmentResource();
    }

    return environment;
  }

  private static final class EnvironmentResource implements AutoCloseable {

    private static final String HOST = "localhost";
    private static final int POSTGRES_PORT = 5432;
    private static final int REDIS_PORT = 6379;
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private final Map<String, String> previousProperties = new LinkedHashMap<>();

    private EnvironmentResource() {
      startEnvironment();
      publishConnectionProperties();
      waitForPort("PostgreSQL", POSTGRES_PORT);
      waitForPort("Redis", REDIS_PORT);
    }

    @Override
    public void close() {
      restoreConnectionProperties();

      if (Boolean.getBoolean("integration.environment.keep-running")) {
        return;
      }

      runDockerCompose("down");
    }

    private void startEnvironment() {
      runDockerCompose("up", "-d");
    }

    private void publishConnectionProperties() {
      setProperty("integration.postgres.jdbc-url", postgresJdbcUrl());
      setProperty("integration.postgres.username", postgresUsername());
      setProperty("integration.postgres.password", postgresPassword());
      setProperty("integration.redis.host", redisHost());
      setProperty("integration.redis.port", String.valueOf(redisPort()));
    }

    private String postgresJdbcUrl() {
      return "jdbc:postgresql://" + HOST + ":" + POSTGRES_PORT + "/products_db";
    }

    private String postgresUsername() {
      return "products_user";
    }

    private String postgresPassword() {
      return "products_pass";
    }

    private String redisHost() {
      return HOST;
    }

    private int redisPort() {
      return REDIS_PORT;
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

    private void waitForPort(String serviceName, int port) {
      var deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
      RuntimeException lastFailure = null;

      while (System.nanoTime() < deadline) {
        try (var socket = new Socket()) {
          socket.connect(new InetSocketAddress(HOST, port), 1_000);
          return;
        } catch (IOException exception) {
          lastFailure =
              new IllegalStateException(serviceName + " did not accept connections yet", exception);
          sleep();
        }
      }

      throw new IllegalStateException(
          serviceName + " was not available on " + HOST + ":" + port, lastFailure);
    }

    private void sleep() {
      try {
        Thread.sleep(1_000);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for Docker Compose services", exception);
      }
    }

    private void runDockerCompose(String... arguments) {
      var command = new java.util.ArrayList<String>();
      command.addAll(List.of("docker", "compose", "-f", composeFile().getAbsolutePath()));
      command.addAll(List.of(arguments));

      try {
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes());
        var exitCode = process.waitFor();

        if (exitCode != 0) {
          throw new IllegalStateException(
              "Docker Compose command failed with exit code " + exitCode + ": " + output);
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Docker Compose command was interrupted", exception);
      } catch (IOException exception) {
        throw new IllegalStateException("Could not execute Docker Compose command", exception);
      }
    }

    private File composeFile() {
      var userDirectory = new File(System.getProperty("user.dir"));
      var projectDirectory =
          "07-cache-aside".equals(userDirectory.getName())
              ? userDirectory
              : new File(userDirectory, "07-cache-aside");

      return new File(projectDirectory, "env/docker-compose.yml");
    }
  }
}
