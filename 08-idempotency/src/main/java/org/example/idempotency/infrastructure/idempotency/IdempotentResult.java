package org.example.idempotency.infrastructure.idempotency;

public record IdempotentResult<T>(int httpStatus, T body, boolean replayed) {

  public static <T> IdempotentResult<T> executed(int httpStatus, T body) {
    return new IdempotentResult<>(httpStatus, body, false);
  }

  public static <T> IdempotentResult<T> replayed(int httpStatus, T body) {
    return new IdempotentResult<>(httpStatus, body, true);
  }
}
