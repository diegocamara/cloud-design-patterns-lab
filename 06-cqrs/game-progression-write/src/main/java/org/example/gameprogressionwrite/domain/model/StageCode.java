package org.example.gameprogressionwrite.domain.model;

import org.example.gameprogressionwrite.domain.exception.DomainException;

public record StageCode(String value) {

  public static final int MAX_CHARACTERS = 100;

  public StageCode {
    if (value == null || value.isBlank()) {
      throw new DomainException("Stage code cannot be blank");
    }

    if (value.length() > MAX_CHARACTERS) {
      throw new DomainException("Stage code cannot be greater than 100 characters");
    }
  }
}
