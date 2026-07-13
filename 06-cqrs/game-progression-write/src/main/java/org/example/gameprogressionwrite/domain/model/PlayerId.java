package org.example.gameprogressionwrite.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PlayerId(UUID value) {

  public PlayerId {
    Objects.requireNonNull(value, "Player id cannot be null");
  }

  public static PlayerId newId() {
    return new PlayerId(UUID.randomUUID());
  }
}
