package org.example.idempotency.infrastructure.idempotency;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.example.idempotency.infrastructure.adapter.repository.springdata.IdempotentRequestJpaRepository;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.IdempotencyStatus;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.IdempotentRequestEntity;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotencyKeyReuseException;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotentRequestInProgressException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class IdempotencyExecutor {

  private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);

  private static final int MAX_KEY_LENGTH = 255;

  private static final TypeReference<Map<String, Object>> RESPONSE_BODY_TYPE =
      new TypeReference<>() {};

  private final IdempotentRequestJpaRepository repository;
  private final RequestHasher requestHasher;
  private final ObjectMapper objectMapper;

  public IdempotencyExecutor(
      IdempotentRequestJpaRepository repository,
      RequestHasher requestHasher,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.requestHasher = requestHasher;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public <T> IdempotentResult<T> execute(
      String operationName,
      String idempotencyKey,
      Object request,
      int successHttpStatus,
      Class<T> responseType,
      Supplier<T> operation) {
    validate(operationName, idempotencyKey, responseType, operation);

    String requestHash = requestHasher.hash(request);

    UUID requestId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    int inserted =
        repository.tryInsert(
            requestId,
            operationName,
            idempotencyKey,
            requestHash,
            now,
            now.plus(DEFAULT_RETENTION));

    if (inserted == 0) {
      return replay(operationName, idempotencyKey, requestHash, responseType);
    }

    /*
     * Any RuntimeException thrown here rolls back:
     *
     * - the idempotency record;
     * - the business operation;
     * - any other database changes in this transaction.
     */
    T response = operation.get();

    Map<String, Object> responseBody =
        response == null ? null : objectMapper.convertValue(response, RESPONSE_BODY_TYPE);

    int updated =
        repository.complete(
            requestId,
            IdempotencyStatus.COMPLETED,
            successHttpStatus,
            responseBody,
            OffsetDateTime.now());

    if (updated != 1) {
      throw new IllegalStateException("Failed to complete idempotent request");
    }

    return IdempotentResult.executed(successHttpStatus, response);
  }

  private <T> IdempotentResult<T> replay(
      String operationName,
      String idempotencyKey,
      String currentRequestHash,
      Class<T> responseType) {
    IdempotentRequestEntity existing =
        repository
            .findByOperationNameAndIdempotencyKey(operationName, idempotencyKey)
            .orElseThrow(
                () -> new IllegalStateException("Idempotent request was not found after conflict"));

    validateRequestHash(existing, currentRequestHash);

    if (!existing.isCompleted()) {
      throw new IdempotentRequestInProgressException();
    }

    T response = deserializeResponse(existing.getResponseBody(), responseType);

    Integer httpStatus = existing.getHttpStatus();

    if (httpStatus == null) {
      throw new IllegalStateException("Completed idempotent request has no HTTP status");
    }

    return IdempotentResult.replayed(httpStatus, response);
  }

  private void validateRequestHash(IdempotentRequestEntity existing, String currentRequestHash) {
    if (!existing.getRequestHash().equals(currentRequestHash)) {
      throw new IdempotencyKeyReuseException();
    }
  }

  private <T> T deserializeResponse(Map<String, Object> responseBody, Class<T> responseType) {
    if (responseBody == null || responseType == Void.class) {
      return null;
    }

    try {
      return objectMapper.convertValue(responseBody, responseType);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Failed to deserialize stored response", exception);
    }
  }

  private void validate(
      String operationName, String idempotencyKey, Class<?> responseType, Supplier<?> operation) {
    if (operationName == null || operationName.isBlank()) {
      throw new IllegalArgumentException("Operation name must not be blank");
    }

    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("Idempotency-Key must not be blank");
    }

    if (idempotencyKey.length() > MAX_KEY_LENGTH) {
      throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");
    }

    Objects.requireNonNull(responseType, "Response type must not be null");

    Objects.requireNonNull(operation, "Operation must not be null");
  }
}
