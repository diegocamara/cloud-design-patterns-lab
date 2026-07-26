package org.example.idempotency.infrastructure.web.errorhandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotencyKeyReuseException;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotentRequestInProgressException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTests {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void shouldReturnConflictWhenIdempotencyKeyIsReusedWithDifferentRequest() {
    var problem = handler.handleIdempotencyKeyReuse(new IdempotencyKeyReuseException());

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("Idempotency key conflict");
    assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:idempotency-key-reuse"));
    assertThat(problem.getDetail())
        .isEqualTo("Idempotency-Key has already been used with a different request");
  }

  @Test
  void shouldReturnConflictWhenIdempotentRequestIsInProgress() {
    var problem =
        handler.handleIdempotentRequestInProgress(new IdempotentRequestInProgressException());

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("Idempotent request in progress");
    assertThat(problem.getType())
        .isEqualTo(URI.create("urn:problem:idempotent-request-in-progress"));
    assertThat(problem.getDetail())
        .isEqualTo("A request with this Idempotency-Key is already being processed");
  }

  @Test
  void shouldReturnBadRequestForInvalidRequest() {
    var problem = handler.handleInvalidRequest(new IllegalArgumentException("Invalid value"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Invalid request");
    assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:invalid-request"));
    assertThat(problem.getDetail()).isEqualTo("Invalid value");
  }
}
