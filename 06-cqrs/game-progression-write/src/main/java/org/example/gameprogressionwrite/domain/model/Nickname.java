package org.example.gameprogressionwrite.domain.model;

import org.example.gameprogressionwrite.domain.exception.DomainException;

public record Nickname(String value) {

  public static final int MAX_CHARACTERS = 100;

  public Nickname {
    if (value == null || value.isBlank()) {
      throw new DomainException("Nickname cannot be blank");
    }

    if (value.length() > MAX_CHARACTERS) {
      throw new DomainException("Nickname cannot be greater than 100 characters");
    }
  }
}
