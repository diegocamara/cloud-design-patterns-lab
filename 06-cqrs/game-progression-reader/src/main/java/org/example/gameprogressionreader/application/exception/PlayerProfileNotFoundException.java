package org.example.gameprogressionreader.application.exception;

import java.util.UUID;

public class PlayerProfileNotFoundException extends RuntimeException {

  public PlayerProfileNotFoundException(UUID playerId) {
    super("Player profile not found: " + playerId);
  }
}
