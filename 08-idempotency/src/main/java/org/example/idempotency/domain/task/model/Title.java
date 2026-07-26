package org.example.idempotency.domain.task.model;

import java.util.Objects;

public record Title(String value) {
  public Title {
    Objects.requireNonNull(value);
    value = value.trim();
  }
}
