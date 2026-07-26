package org.example.idempotency.infrastructure.web.errorhandler;

import java.net.URI;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotencyKeyReuseException;
import org.example.idempotency.infrastructure.idempotency.exception.IdempotentRequestInProgressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IdempotencyKeyReuseException.class)
  public ProblemDetail handleIdempotencyKeyReuse(IdempotencyKeyReuseException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "Idempotency key conflict",
        "urn:problem:idempotency-key-reuse",
        exception.getMessage());
  }

  @ExceptionHandler(IdempotentRequestInProgressException.class)
  public ProblemDetail handleIdempotentRequestInProgress(
      IdempotentRequestInProgressException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "Idempotent request in progress",
        "urn:problem:idempotent-request-in-progress",
        exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid request",
        "urn:problem:invalid-request",
        exception.getMessage());
  }

  private ProblemDetail problem(
      HttpStatus status, String title, String type, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create(type));
    return problem;
  }
}
