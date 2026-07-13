package org.example.gameprogressionreader.application.query.handler;

import java.util.Objects;
import java.util.UUID;
import org.example.gameprogressionreader.application.exception.PlayerProfileNotFoundException;
import org.example.gameprogressionreader.application.port.PlayerProfileQueryRepository;
import org.example.gameprogressionreader.application.query.model.PlayerProfileView;

public final class GetPlayerProfileQueryHandler {

  private final PlayerProfileQueryRepository playerProfileQueryRepository;

  public GetPlayerProfileQueryHandler(PlayerProfileQueryRepository playerProfileQueryRepository) {
    this.playerProfileQueryRepository = playerProfileQueryRepository;
  }

  public PlayerProfileView handle(UUID playerId) {
    Objects.requireNonNull(playerId, "Player id cannot be null");

    return this.playerProfileQueryRepository
        .findById(playerId)
        .orElseThrow(() -> new PlayerProfileNotFoundException(playerId));
  }
}
