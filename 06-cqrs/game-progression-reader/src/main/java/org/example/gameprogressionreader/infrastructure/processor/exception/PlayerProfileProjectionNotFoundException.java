package org.example.gameprogressionreader.infrastructure.processor.exception;

import java.util.UUID;

public class PlayerProfileProjectionNotFoundException extends RuntimeException {

  public PlayerProfileProjectionNotFoundException(UUID playerId) {
    super("Player profile projection not found: " + playerId);
  }
}
