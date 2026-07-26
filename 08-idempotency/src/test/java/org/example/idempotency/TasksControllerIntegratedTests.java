package org.example.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.idempotency.application.task.model.TaskCreatorInput;
import org.example.idempotency.environment.PostgresClient;
import org.example.idempotency.environment.PostgreSQLExtension;
import org.example.idempotency.environment.RabbitMQClientExtension;
import org.example.idempotency.infrastructure.idempotency.RequestHasher;
import org.example.idempotency.infrastructure.web.model.request.CreateTaskRequest;
import org.example.idempotency.infrastructure.web.model.response.CreateTaskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TasksControllerIntegratedTests {

  @RegisterExtension static final PostgreSQLExtension POSTGRESQL = new PostgreSQLExtension();

  @RegisterExtension static final RabbitMQClientExtension RABBITMQ = new RabbitMQClientExtension();

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort private int port;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private RequestHasher requestHasher;

  @DynamicPropertySource
  static void configureInfrastructure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL::jdbcUrl);
    registry.add("spring.datasource.username", POSTGRESQL::username);
    registry.add("spring.datasource.password", POSTGRESQL::password);
    registry.add("spring.rabbitmq.host", RABBITMQ::host);
    registry.add("spring.rabbitmq.port", RABBITMQ::amqpPort);
    registry.add("spring.rabbitmq.username", RABBITMQ::username);
    registry.add("spring.rabbitmq.password", RABBITMQ::password);
    registry.add("spring.rabbitmq.virtual-host", RABBITMQ::virtualHost);
  }

  @Test
  void shouldCreateTaskOnFirstRequest(PostgresClient postgres) throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var title = "Review idempotency implementation";

    var response = createTask(idempotencyKey, new CreateTaskRequest(title));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(response.headers().firstValue("Idempotent-Replayed")).contains("false");

    var responseBody = objectMapper.readValue(response.body(), CreateTaskResponse.class);
    assertThat(responseBody.id()).isNotNull();
    assertThat(responseBody.title()).isEqualTo(title);

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(responseBody.id());
              assertThat(task.get("title")).isEqualTo(title);
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT operation_name,
                       idempotency_key,
                       request_hash,
                       status,
                       http_status,
                       response_body,
                       completed_at,
                       expires_at
                  FROM idempotent_requests
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("operation_name")).isEqualTo("CREATE_TASK");
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("request_hash").toString()).hasSize(64);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(responseBody.id().toString())
                  .contains(title);
              assertThat(idempotentRequest.get("completed_at")).isNotNull();
              assertThat(idempotentRequest.get("expires_at")).isNotNull();
            });
  }

  @Test
  void shouldReplayPreviousResponseForSameKeyAndPayload(PostgresClient postgres)
      throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Document idempotency behavior");

    var firstResponse = createTask(idempotencyKey, request);
    var idempotentRequestBeforeReplay =
        postgres
            .queryForList(
                """
                SELECT request_hash, response_body, completed_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey)
            .getFirst();

    var replayedResponse = createTask(idempotencyKey, request);

    assertThat(firstResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(firstResponse.headers().firstValue("Idempotent-Replayed")).contains("false");
    assertThat(replayedResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(replayedResponse.headers().firstValue("Idempotent-Replayed")).contains("true");

    var firstResponseBody =
        objectMapper.readValue(firstResponse.body(), CreateTaskResponse.class);
    var replayedResponseBody =
        objectMapper.readValue(replayedResponse.body(), CreateTaskResponse.class);

    assertThat(replayedResponseBody).isEqualTo(firstResponseBody);
    assertThat(replayedResponseBody.title()).isEqualTo(request.title());

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(firstResponseBody.id());
              assertThat(task.get("title")).isEqualTo(request.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT request_hash, response_body, completed_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey))
        .singleElement()
        .satisfies(
            idempotentRequestAfterReplay -> {
              assertThat(idempotentRequestAfterReplay.get("request_hash"))
                  .isEqualTo(idempotentRequestBeforeReplay.get("request_hash"));
              assertThat(idempotentRequestAfterReplay.get("response_body").toString())
                  .isEqualTo(idempotentRequestBeforeReplay.get("response_body").toString());
              assertThat(idempotentRequestAfterReplay.get("completed_at"))
                  .isEqualTo(idempotentRequestBeforeReplay.get("completed_at"));
            });
  }

  @Test
  void shouldRejectSameKeyWithDifferentPayload(PostgresClient postgres) throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var originalRequest = new CreateTaskRequest("Implement task creation");
    var conflictingRequest = new CreateTaskRequest("Delete task creation");

    var originalResponse = createTask(idempotencyKey, originalRequest);
    var idempotentRequestBeforeConflict =
        postgres
            .queryForList(
                """
                SELECT request_hash, response_body, completed_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey)
            .getFirst();

    var conflictingResponse = createTask(idempotencyKey, conflictingRequest);

    assertThat(originalResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(conflictingResponse.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(conflictingResponse.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).startsWith("application/problem+json"));
    assertThat(conflictingResponse.headers().firstValue("Idempotent-Replayed")).isEmpty();

    var problem = objectMapper.readTree(conflictingResponse.body());
    assertThat(problem.get("type").stringValue())
        .isEqualTo("urn:problem:idempotency-key-reuse");
    assertThat(problem.get("title").stringValue()).isEqualTo("Idempotency key conflict");
    assertThat(problem.get("status").asInt()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.get("detail").stringValue())
        .isEqualTo("Idempotency-Key has already been used with a different request");

    var originalResponseBody =
        objectMapper.readValue(originalResponse.body(), CreateTaskResponse.class);

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(originalResponseBody.id());
              assertThat(task.get("title")).isEqualTo(originalRequest.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT request_hash, response_body, completed_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey))
        .singleElement()
        .satisfies(
            idempotentRequestAfterConflict -> {
              assertThat(idempotentRequestAfterConflict.get("request_hash"))
                  .isEqualTo(idempotentRequestBeforeConflict.get("request_hash"));
              assertThat(idempotentRequestAfterConflict.get("response_body").toString())
                  .isEqualTo(idempotentRequestBeforeConflict.get("response_body").toString());
              assertThat(idempotentRequestAfterConflict.get("completed_at"))
                  .isEqualTo(idempotentRequestBeforeConflict.get("completed_at"));
            });
  }

  @Test
  void shouldCreateDifferentTasksForDifferentKeys(PostgresClient postgres) throws Exception {
    var firstIdempotencyKey = UUID.randomUUID().toString();
    var secondIdempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Test independent idempotency keys");

    var firstResponse = createTask(firstIdempotencyKey, request);
    var secondResponse = createTask(secondIdempotencyKey, request);

    assertThat(firstResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(firstResponse.headers().firstValue("Idempotent-Replayed")).contains("false");
    assertThat(secondResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(secondResponse.headers().firstValue("Idempotent-Replayed")).contains("false");

    var firstResponseBody =
        objectMapper.readValue(firstResponse.body(), CreateTaskResponse.class);
    var secondResponseBody =
        objectMapper.readValue(secondResponse.body(), CreateTaskResponse.class);

    assertThat(firstResponseBody.id()).isNotEqualTo(secondResponseBody.id());
    assertThat(firstResponseBody.title()).isEqualTo(request.title());
    assertThat(secondResponseBody.title()).isEqualTo(request.title());

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .hasSize(2)
        .allSatisfy(task -> assertThat(task.get("title")).isEqualTo(request.title()))
        .extracting(task -> task.get("id"))
        .containsExactlyInAnyOrder(firstResponseBody.id(), secondResponseBody.id());

    var idempotentRequests =
        postgres.queryForList(
            """
            SELECT idempotency_key, request_hash, status, http_status, response_body
              FROM idempotent_requests
             WHERE operation_name = 'CREATE_TASK'
            """);

    assertThat(idempotentRequests)
        .hasSize(2)
        .allSatisfy(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
            })
        .extracting(idempotentRequest -> idempotentRequest.get("idempotency_key"))
        .containsExactlyInAnyOrder(firstIdempotencyKey, secondIdempotencyKey);

    var requestHashes =
        idempotentRequests.stream()
            .map(idempotentRequest -> idempotentRequest.get("request_hash").toString())
            .collect(java.util.stream.Collectors.toSet());
    assertThat(requestHashes).hasSize(1).allSatisfy(hash -> assertThat(hash).hasSize(64));

    assertThat(idempotentRequests)
        .anySatisfy(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key"))
                  .isEqualTo(firstIdempotencyKey);
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(firstResponseBody.id().toString());
            })
        .anySatisfy(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key"))
                  .isEqualTo(secondIdempotencyKey);
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(secondResponseBody.id().toString());
            });
  }

  @Test
  void shouldReplayResponseOnMultipleSequentialRequests(PostgresClient postgres)
      throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Repeat task creation safely");
    var responses = new ArrayList<HttpResponse<String>>();
    var responseBodies = new ArrayList<CreateTaskResponse>();

    for (int attempt = 0; attempt < 5; attempt++) {
      var response = createTask(idempotencyKey, request);
      responses.add(response);
      responseBodies.add(objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }

    assertThat(responses)
        .allSatisfy(
            response ->
                assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()));
    assertThat(responses.getFirst().headers().firstValue("Idempotent-Replayed"))
        .contains("false");
    assertThat(responses.subList(1, responses.size()))
        .allSatisfy(
            response ->
                assertThat(response.headers().firstValue("Idempotent-Replayed"))
                    .contains("true"));

    var originalResponseBody = responseBodies.getFirst();
    assertThat(responseBodies)
        .containsOnly(originalResponseBody)
        .allSatisfy(responseBody -> assertThat(responseBody.title()).isEqualTo(request.title()));

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(originalResponseBody.id());
              assertThat(task.get("title")).isEqualTo(request.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT idempotency_key, status, http_status, response_body
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(originalResponseBody.id().toString())
                  .contains(request.title());
            });
  }

  @Test
  void shouldHandleConcurrentRequestsWithSameKey(PostgresClient postgres) throws Exception {
    var totalRequests = 100;
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Create task concurrently");
    var startSignal = new CountDownLatch(1);
    var responses = new ArrayList<HttpResponse<String>>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          java.util.stream.IntStream.range(0, totalRequests)
              .mapToObj(
                  ignored ->
                      executor.submit(
                          () -> {
                            startSignal.await();
                            return createTask(
                                idempotencyKey, request, Duration.ofSeconds(30));
                          }))
              .toList();

      startSignal.countDown();

      for (var future : futures) {
        responses.add(future.get(35, TimeUnit.SECONDS));
      }
    }

    assertThat(responses)
        .hasSize(totalRequests)
        .allSatisfy(
            response -> {
              assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
              assertThat(response.headers().firstValue("Idempotent-Replayed")).isPresent();
            });
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("false"))
        .hasSize(1);
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("true"))
        .hasSize(totalRequests - 1);

    var responseBodies = new ArrayList<CreateTaskResponse>();
    for (var response : responses) {
      responseBodies.add(objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }

    var createdTask = responseBodies.getFirst();
    assertThat(responseBodies)
        .containsOnly(createdTask)
        .allSatisfy(responseBody -> assertThat(responseBody.title()).isEqualTo(request.title()));

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(createdTask.id());
              assertThat(task.get("title")).isEqualTo(request.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT idempotency_key, status, http_status, response_body
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(createdTask.id().toString());
            });
  }

  @Test
  void shouldRejectConcurrentRequestsWithSameKeyAndDifferentPayloads(PostgresClient postgres)
      throws Exception {
    var totalRequests = 100;
    var idempotencyKey = UUID.randomUUID().toString();
    var firstRequest = new CreateTaskRequest("Concurrent payload A");
    var secondRequest = new CreateTaskRequest("Concurrent payload B");
    var requests =
        java.util.stream.IntStream.range(0, totalRequests)
            .mapToObj(index -> index % 2 == 0 ? firstRequest : secondRequest)
            .toList();
    var startSignal = new CountDownLatch(1);
    var responses = new ArrayList<HttpResponse<String>>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          requests.stream()
              .map(
                  request ->
                      executor.submit(
                          () -> {
                            startSignal.await();
                            return createTask(
                                idempotencyKey, request, Duration.ofSeconds(30));
                          }))
              .toList();

      startSignal.countDown();

      for (var future : futures) {
        responses.add(future.get(35, TimeUnit.SECONDS));
      }
    }

    var successfulResponses =
        responses.stream()
            .filter(response -> response.statusCode() == HttpStatus.CREATED.value())
            .toList();
    var conflictResponses =
        responses.stream()
            .filter(response -> response.statusCode() == HttpStatus.CONFLICT.value())
            .toList();

    assertThat(successfulResponses).hasSize(totalRequests / 2);
    assertThat(conflictResponses).hasSize(totalRequests / 2);
    assertThat(successfulResponses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("false"))
        .hasSize(1);
    assertThat(successfulResponses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("true"))
        .hasSize(totalRequests / 2 - 1);

    var successfulResponseBodies = new ArrayList<CreateTaskResponse>();
    for (var response : successfulResponses) {
      successfulResponseBodies.add(
          objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }

    var createdTask = successfulResponseBodies.getFirst();
    var losingTitle =
        createdTask.title().equals(firstRequest.title())
            ? secondRequest.title()
            : firstRequest.title();

    assertThat(successfulResponseBodies).containsOnly(createdTask);

    for (var index = 0; index < totalRequests; index++) {
      var request = requests.get(index);
      var response = responses.get(index);

      if (request.title().equals(createdTask.title())) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
      } else {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();

        var problem = objectMapper.readTree(response.body());
        assertThat(problem.get("type").stringValue())
            .isEqualTo("urn:problem:idempotency-key-reuse");
        assertThat(problem.get("status").asInt()).isEqualTo(HttpStatus.CONFLICT.value());
      }
    }

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(createdTask.id());
              assertThat(task.get("title")).isEqualTo(createdTask.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT idempotency_key, request_hash, status, http_status, response_body
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("request_hash").toString()).hasSize(64);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(createdTask.id().toString())
                  .contains(createdTask.title())
                  .doesNotContain(losingTitle);
            });
  }

  @Test
  void shouldRollbackIdempotencyRecordAndAllowRetryAfterOperationFailure(PostgresClient postgres)
      throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Retry task after transient failure");

    postgres.update(
        """
        ALTER TABLE tasks
          ADD CONSTRAINT ck_tasks_scenario_8_transient_failure
        CHECK (title <> 'Retry task after transient failure')
        """);

    HttpResponse<String> failedResponse;
    try {
      failedResponse = createTask(idempotencyKey, request);
    } finally {
      postgres.update(
          """
          ALTER TABLE tasks
          DROP CONSTRAINT IF EXISTS ck_tasks_scenario_8_transient_failure
          """);
    }

    assertThat(failedResponse.statusCode())
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(failedResponse.headers().firstValue("Idempotent-Replayed")).isEmpty();
    assertThat(postgres.queryForList("SELECT id FROM tasks")).isEmpty();
    assertThat(
            postgres.queryForList(
                """
                SELECT id
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey))
        .isEmpty();

    var retriedResponse = createTask(idempotencyKey, request);

    assertThat(retriedResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(retriedResponse.headers().firstValue("Idempotent-Replayed")).contains("false");

    var createdTask =
        objectMapper.readValue(retriedResponse.body(), CreateTaskResponse.class);
    assertThat(createdTask.title()).isEqualTo(request.title());

    assertThat(postgres.queryForList("SELECT id, title FROM tasks"))
        .singleElement()
        .satisfies(
            task -> {
              assertThat(task.get("id")).isEqualTo(createdTask.id());
              assertThat(task.get("title")).isEqualTo(request.title());
            });

    assertThat(
            postgres.queryForList(
                """
                SELECT idempotency_key, status, http_status, response_body
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(createdTask.id().toString())
                  .contains(request.title());
            });
  }

  @Test
  void shouldRejectRequestWhileSameIdempotencyKeyIsProcessing(PostgresClient postgres)
      throws Exception {
    var idempotencyRequestId = UUID.randomUUID();
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Task already being processed");
    var requestHash = requestHasher.hash(new TaskCreatorInput(request.title()));
    var createdAt = OffsetDateTime.now().minusSeconds(5);
    var expiresAt = createdAt.plusHours(24);

    postgres.update(
        """
        INSERT INTO idempotent_requests (
            id,
            operation_name,
            idempotency_key,
            request_hash,
            status,
            created_at,
            expires_at
        )
        VALUES (?, 'CREATE_TASK', ?, ?, 'PROCESSING', ?, ?)
        """,
        idempotencyRequestId,
        idempotencyKey,
        requestHash,
        createdAt,
        expiresAt);

    var response = createTask(idempotencyKey, request);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).startsWith("application/problem+json"));
    assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();

    var problem = objectMapper.readTree(response.body());
    assertThat(problem.get("type").stringValue())
        .isEqualTo("urn:problem:idempotent-request-in-progress");
    assertThat(problem.get("title").stringValue()).isEqualTo("Idempotent request in progress");
    assertThat(problem.get("status").asInt()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.get("detail").stringValue())
        .isEqualTo("A request with this Idempotency-Key is already being processed");

    assertThat(postgres.queryForList("SELECT id FROM tasks")).isEmpty();
    assertThat(
            postgres.queryForList(
                """
                SELECT id,
                       idempotency_key,
                       request_hash,
                       status,
                       http_status,
                       response_body,
                       completed_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("id")).isEqualTo(idempotencyRequestId);
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("request_hash")).isEqualTo(requestHash);
              assertThat(idempotentRequest.get("status")).isEqualTo("PROCESSING");
              assertThat(idempotentRequest.get("http_status")).isNull();
              assertThat(idempotentRequest.get("response_body")).isNull();
              assertThat(idempotentRequest.get("completed_at")).isNull();
            });
  }

  @Test
  void shouldExecuteAgainWhenIdempotencyKeyHasExpired(PostgresClient postgres) throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();
    var firstRequest = new CreateTaskRequest("Task before key expiration");
    var secondRequest = new CreateTaskRequest("Task after key expiration");

    var firstResponse = createTask(idempotencyKey, firstRequest);
    var firstResponseBody =
        objectMapper.readValue(firstResponse.body(), CreateTaskResponse.class);
    var firstIdempotentRequest =
        postgres
            .queryForList(
                """
                SELECT id, request_hash, response_body, created_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                   AND idempotency_key = ?
                """,
                idempotencyKey)
            .getFirst();

    postgres.update(
        """
        UPDATE idempotent_requests
           SET expires_at = ?
         WHERE operation_name = 'CREATE_TASK'
           AND idempotency_key = ?
        """,
        OffsetDateTime.now().minusSeconds(1),
        idempotencyKey);

    var secondResponse = createTask(idempotencyKey, secondRequest);

    assertThat(firstResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(firstResponse.headers().firstValue("Idempotent-Replayed")).contains("false");
    assertThat(secondResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(secondResponse.headers().firstValue("Idempotent-Replayed")).contains("false");

    var secondResponseBody =
        objectMapper.readValue(secondResponse.body(), CreateTaskResponse.class);
    assertThat(secondResponseBody.id()).isNotEqualTo(firstResponseBody.id());
    assertThat(secondResponseBody.title()).isEqualTo(secondRequest.title());

    var tasks = postgres.queryForList("SELECT id, title FROM tasks");
    assertThat(tasks).hasSize(2);
    assertThat(tasks)
        .extracting(task -> task.get("id"))
        .containsExactlyInAnyOrder(firstResponseBody.id(), secondResponseBody.id());
    assertThat(tasks)
        .extracting(task -> task.get("title"))
        .containsExactlyInAnyOrder(firstRequest.title(), secondRequest.title());

    assertThat(
            postgres.queryForList(
                """
                SELECT id,
                       idempotency_key,
                       request_hash,
                       status,
                       http_status,
                       response_body,
                       created_at,
                       completed_at,
                       expires_at
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("id"))
                  .isNotEqualTo(firstIdempotentRequest.get("id"));
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("request_hash"))
                  .isNotEqualTo(firstIdempotentRequest.get("request_hash"));
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(secondResponseBody.id().toString())
                  .contains(secondRequest.title())
                  .doesNotContain(firstResponseBody.id().toString())
                  .doesNotContain(firstRequest.title());
              assertThat(idempotentRequest.get("created_at"))
                  .isNotEqualTo(firstIdempotentRequest.get("created_at"));
              assertThat(idempotentRequest.get("completed_at")).isNotNull();
              assertThat(idempotentRequest.get("expires_at")).isNotNull();
            });
  }

  @Test
  void shouldRejectMissingOrBlankIdempotencyKey(PostgresClient postgres) throws Exception {
    var requestBody = objectMapper.writeValueAsString(new CreateTaskRequest("Invalid key"));

    var missingKeyResponse =
        sendTaskRequest(
            null, HttpRequest.BodyPublishers.ofString(requestBody), Duration.ofSeconds(10));
    var blankKeyResponse =
        sendTaskRequest(
            "   ", HttpRequest.BodyPublishers.ofString(requestBody), Duration.ofSeconds(10));

    assertThat(List.of(missingKeyResponse, blankKeyResponse))
        .allSatisfy(
            response -> {
              assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();
            });
    assertTaskAndIdempotencyTablesAreEmpty(postgres);
  }

  @Test
  void shouldRejectIdempotencyKeyLongerThan255Characters(PostgresClient postgres)
      throws Exception {
    var response =
        createTask("a".repeat(256), new CreateTaskRequest("Invalid long idempotency key"));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();

    var problem = objectMapper.readTree(response.body());
    assertThat(problem.get("type").stringValue()).isEqualTo("urn:problem:invalid-request");
    assertThat(problem.get("title").stringValue()).isEqualTo("Invalid request");
    assertThat(problem.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.get("detail").stringValue())
        .isEqualTo("Idempotency-Key must not exceed 255 characters");

    assertTaskAndIdempotencyTablesAreEmpty(postgres);
  }

  @Test
  void shouldRejectInvalidTaskPayloadBeforeReservingIdempotencyKey(PostgresClient postgres)
      throws Exception {
    var invalidRequests =
        List.of(
            new CreateTaskRequest(null),
            new CreateTaskRequest(""),
            new CreateTaskRequest("   "));
    var responses = new ArrayList<HttpResponse<String>>();

    for (var request : invalidRequests) {
      responses.add(createTask(UUID.randomUUID().toString(), request));
    }

    assertThat(responses)
        .allSatisfy(
            response -> {
              assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();
            });
    assertTaskAndIdempotencyTablesAreEmpty(postgres);
  }

  @Test
  void shouldKeepSameIdempotencyKeyIndependentAcrossOperations(PostgresClient postgres)
      throws Exception {
    var idempotencyKey = UUID.randomUUID().toString();

    postgres.update(
        """
        INSERT INTO idempotent_requests (
            id,
            operation_name,
            idempotency_key,
            request_hash,
            status,
            created_at,
            expires_at
        )
        VALUES (?, 'ANOTHER_OPERATION', ?, ?, 'PROCESSING', ?, ?)
        """,
        UUID.randomUUID(),
        idempotencyKey,
        "0".repeat(64),
        OffsetDateTime.now(),
        OffsetDateTime.now().plusHours(24));

    var response =
        createTask(idempotencyKey, new CreateTaskRequest("Independent operation key"));

    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(response.headers().firstValue("Idempotent-Replayed")).contains("false");
    assertThat(postgres.queryForList("SELECT id FROM tasks")).hasSize(1);
    assertThat(
            postgres.queryForList(
                """
                SELECT operation_name, idempotency_key, status
                  FROM idempotent_requests
                 ORDER BY operation_name
                """))
        .hasSize(2)
        .allSatisfy(
            idempotentRequest ->
                assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey))
        .extracting(idempotentRequest -> idempotentRequest.get("operation_name"))
        .containsExactly("ANOTHER_OPERATION", "CREATE_TASK");
  }

  @Test
  void shouldRejectMalformedOrMissingRequestBodyBeforeReservingIdempotencyKey(
      PostgresClient postgres) throws Exception {
    var malformedResponse =
        sendTaskRequest(
            UUID.randomUUID().toString(),
            HttpRequest.BodyPublishers.ofString("{\"title\":"),
            Duration.ofSeconds(10));
    var missingBodyResponse =
        sendTaskRequest(
            UUID.randomUUID().toString(),
            HttpRequest.BodyPublishers.noBody(),
            Duration.ofSeconds(10));

    assertThat(List.of(malformedResponse, missingBodyResponse))
        .allSatisfy(
            response -> {
              assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
              assertThat(response.headers().firstValue("Idempotent-Replayed")).isEmpty();
            });
    assertTaskAndIdempotencyTablesAreEmpty(postgres);
  }

  @Test
  void shouldExecuteOnlyOnceWhenExpiredKeyIsRetriedConcurrently(PostgresClient postgres)
      throws Exception {
    var totalRequests = 100;
    var idempotencyKey = UUID.randomUUID().toString();
    var previousRequest = new CreateTaskRequest("Task before concurrent expiration retry");
    var retriedRequest = new CreateTaskRequest("Task after concurrent expiration retry");

    var previousResponse = createTask(idempotencyKey, previousRequest);
    var previousResponseBody =
        objectMapper.readValue(previousResponse.body(), CreateTaskResponse.class);

    postgres.update(
        """
        UPDATE idempotent_requests
           SET expires_at = ?
         WHERE operation_name = 'CREATE_TASK'
           AND idempotency_key = ?
        """,
        OffsetDateTime.now().minusSeconds(1),
        idempotencyKey);

    var requests =
        java.util.stream.IntStream.range(0, totalRequests)
            .mapToObj(ignored -> retriedRequest)
            .toList();
    var responses = createTasksConcurrently(idempotencyKey, requests);

    assertThat(responses)
        .hasSize(totalRequests)
        .allSatisfy(
            response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()));
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("false"))
        .hasSize(1);
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("true"))
        .hasSize(totalRequests - 1);

    var responseBodies = new ArrayList<CreateTaskResponse>();
    for (var response : responses) {
      responseBodies.add(objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }
    var retriedResponseBody = responseBodies.getFirst();
    assertThat(responseBodies).containsOnly(retriedResponseBody);
    assertThat(retriedResponseBody.id()).isNotEqualTo(previousResponseBody.id());
    assertThat(retriedResponseBody.title()).isEqualTo(retriedRequest.title());

    var tasks = postgres.queryForList("SELECT id, title FROM tasks");
    assertThat(tasks).hasSize(2);
    assertThat(tasks)
        .extracting(task -> task.get("id"))
        .containsExactlyInAnyOrder(previousResponseBody.id(), retriedResponseBody.id());
    assertThat(postgres.queryForList("SELECT id FROM idempotent_requests")).hasSize(1);
  }

  @Test
  void shouldExecuteOnlyOnceWhenRetriedConcurrentlyAfterFailure(PostgresClient postgres)
      throws Exception {
    var totalRequests = 100;
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new CreateTaskRequest("Concurrent retry after transient failure");

    postgres.update(
        """
        ALTER TABLE tasks
          ADD CONSTRAINT ck_tasks_scenario_17_transient_failure
        CHECK (title <> 'Concurrent retry after transient failure')
        """);

    HttpResponse<String> failedResponse;
    try {
      failedResponse = createTask(idempotencyKey, request);
    } finally {
      postgres.update(
          """
          ALTER TABLE tasks
          DROP CONSTRAINT IF EXISTS ck_tasks_scenario_17_transient_failure
          """);
    }

    assertThat(failedResponse.statusCode())
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertTaskAndIdempotencyTablesAreEmpty(postgres);

    var requests =
        java.util.stream.IntStream.range(0, totalRequests)
            .mapToObj(ignored -> request)
            .toList();
    var responses = createTasksConcurrently(idempotencyKey, requests);

    assertThat(responses)
        .hasSize(totalRequests)
        .allSatisfy(
            response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()));
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("false"))
        .hasSize(1);
    assertThat(responses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("true"))
        .hasSize(totalRequests - 1);

    var responseBodies = new ArrayList<CreateTaskResponse>();
    for (var response : responses) {
      responseBodies.add(objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }
    assertThat(responseBodies).containsOnly(responseBodies.getFirst());
    assertThat(postgres.queryForList("SELECT id, title FROM tasks")).singleElement();
    assertThat(
            postgres.queryForList(
                """
                SELECT idempotency_key, status, http_status
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("idempotency_key")).isEqualTo(idempotencyKey);
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("http_status"))
                  .isEqualTo(HttpStatus.CREATED.value());
            });
  }

  @Test
  void shouldAllowOnlyOnePayloadWhenExpiredKeyIsRetriedConcurrently(PostgresClient postgres)
      throws Exception {
    var totalRequests = 100;
    var idempotencyKey = UUID.randomUUID().toString();
    var previousRequest = new CreateTaskRequest("Task before expiration race");
    var firstRequest = new CreateTaskRequest("Expired key payload A");
    var secondRequest = new CreateTaskRequest("Expired key payload B");

    var previousResponse = createTask(idempotencyKey, previousRequest);
    var previousResponseBody =
        objectMapper.readValue(previousResponse.body(), CreateTaskResponse.class);

    postgres.update(
        """
        UPDATE idempotent_requests
           SET expires_at = ?
         WHERE operation_name = 'CREATE_TASK'
           AND idempotency_key = ?
        """,
        OffsetDateTime.now().minusSeconds(1),
        idempotencyKey);

    var requests =
        java.util.stream.IntStream.range(0, totalRequests)
            .mapToObj(index -> index % 2 == 0 ? firstRequest : secondRequest)
            .toList();
    var responses = createTasksConcurrently(idempotencyKey, requests);
    var successfulResponses =
        responses.stream()
            .filter(response -> response.statusCode() == HttpStatus.CREATED.value())
            .toList();
    var conflictResponses =
        responses.stream()
            .filter(response -> response.statusCode() == HttpStatus.CONFLICT.value())
            .toList();

    assertThat(successfulResponses).hasSize(totalRequests / 2);
    assertThat(conflictResponses).hasSize(totalRequests / 2);
    assertThat(successfulResponses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("false"))
        .hasSize(1);
    assertThat(successfulResponses)
        .filteredOn(
            response ->
                response.headers().firstValue("Idempotent-Replayed").orElseThrow().equals("true"))
        .hasSize(totalRequests / 2 - 1);

    var successfulResponseBodies = new ArrayList<CreateTaskResponse>();
    for (var response : successfulResponses) {
      successfulResponseBodies.add(
          objectMapper.readValue(response.body(), CreateTaskResponse.class));
    }
    var winningResponse = successfulResponseBodies.getFirst();
    assertThat(successfulResponseBodies).containsOnly(winningResponse);

    for (var index = 0; index < totalRequests; index++) {
      var response = responses.get(index);
      if (requests.get(index).title().equals(winningResponse.title())) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
      } else {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        var problem = objectMapper.readTree(response.body());
        assertThat(problem.get("type").stringValue())
            .isEqualTo("urn:problem:idempotency-key-reuse");
      }
    }

    var tasks = postgres.queryForList("SELECT id, title FROM tasks");
    assertThat(tasks).hasSize(2);
    assertThat(tasks)
        .extracting(task -> task.get("id"))
        .containsExactlyInAnyOrder(previousResponseBody.id(), winningResponse.id());
    assertThat(tasks)
        .extracting(task -> task.get("title"))
        .containsExactlyInAnyOrder(previousRequest.title(), winningResponse.title());
    assertThat(
            postgres.queryForList(
                """
                SELECT status, response_body
                  FROM idempotent_requests
                 WHERE operation_name = 'CREATE_TASK'
                """))
        .singleElement()
        .satisfies(
            idempotentRequest -> {
              assertThat(idempotentRequest.get("status")).isEqualTo("COMPLETED");
              assertThat(idempotentRequest.get("response_body").toString())
                  .contains(winningResponse.id().toString())
                  .contains(winningResponse.title())
                  .doesNotContain(previousResponseBody.id().toString());
            });
  }

  private HttpResponse<String> createTask(String idempotencyKey, CreateTaskRequest body)
      throws Exception {
    return createTask(idempotencyKey, body, Duration.ofSeconds(10));
  }

  private HttpResponse<String> createTask(
      String idempotencyKey, CreateTaskRequest body, Duration timeout) throws Exception {
    return sendTaskRequest(
        idempotencyKey,
        HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body)),
        timeout);
  }

  private HttpResponse<String> sendTaskRequest(
      String idempotencyKey, HttpRequest.BodyPublisher body, Duration timeout) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/tasks"))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(body);

    if (idempotencyKey != null) {
      request.header("Idempotency-Key", idempotencyKey);
    }

    return this.httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private List<HttpResponse<String>> createTasksConcurrently(
      String idempotencyKey, List<CreateTaskRequest> requests) throws Exception {
    var startSignal = new CountDownLatch(1);
    var responses = new ArrayList<HttpResponse<String>>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          requests.stream()
              .map(
                  request ->
                      executor.submit(
                          () -> {
                            startSignal.await();
                            return createTask(
                                idempotencyKey, request, Duration.ofSeconds(30));
                          }))
              .toList();

      startSignal.countDown();

      for (var future : futures) {
        responses.add(future.get(35, TimeUnit.SECONDS));
      }
    }

    return responses;
  }

  private void assertTaskAndIdempotencyTablesAreEmpty(PostgresClient postgres) {
    assertThat(postgres.queryForList("SELECT id FROM tasks")).isEmpty();
    assertThat(postgres.queryForList("SELECT id FROM idempotent_requests")).isEmpty();
  }
}
