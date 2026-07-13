package org.example.gameprogressionwrite.application.exception;

import org.example.gameprogressionwrite.domain.model.PlayerId;

public class PlayerNotFoundException extends RuntimeException {
  public PlayerNotFoundException(PlayerId playerId) {
    super("Player " + playerId + " not found");
  }
}
