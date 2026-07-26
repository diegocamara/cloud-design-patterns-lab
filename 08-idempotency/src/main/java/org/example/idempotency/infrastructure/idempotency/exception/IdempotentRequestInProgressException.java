package org.example.idempotency.infrastructure.idempotency.exception;

public class IdempotentRequestInProgressException extends RuntimeException {

  public IdempotentRequestInProgressException() {
    super("A request with this Idempotency-Key is already being processed");
  }
}
