package org.example.cacheaside.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Id(UUID value) {
  public Id {
    Objects.requireNonNull(value);
  }
}
