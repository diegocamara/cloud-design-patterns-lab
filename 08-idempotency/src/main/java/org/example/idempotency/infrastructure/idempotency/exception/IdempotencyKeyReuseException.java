package org.example.idempotency.infrastructure.idempotency.exception;

public class IdempotencyKeyReuseException extends RuntimeException {

  public IdempotencyKeyReuseException() {
    super("Idempotency-Key has already been used with a different request");
  }
}
