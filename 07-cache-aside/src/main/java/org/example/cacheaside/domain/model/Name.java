package org.example.cacheaside.domain.model;

import java.util.Objects;

public record Name(String value) {
  public Name {
    Objects.requireNonNull(value);
    value = value.trim();
  }
}
