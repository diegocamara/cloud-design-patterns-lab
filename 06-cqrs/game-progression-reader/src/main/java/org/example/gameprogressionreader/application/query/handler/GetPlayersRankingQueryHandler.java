package org.example.gameprogressionreader.application.query.handler;

import java.util.List;
import org.example.gameprogressionreader.application.port.PlayerProfileQueryRepository;
import org.example.gameprogressionreader.application.query.model.PlayerRankingItem;

public final class GetPlayersRankingQueryHandler {

  private static final int MAX_LIMIT = 100;

  private final PlayerProfileQueryRepository playerProfileQueryRepository;

  public GetPlayersRankingQueryHandler(PlayerProfileQueryRepository playerProfileQueryRepository) {
    this.playerProfileQueryRepository = playerProfileQueryRepository;
  }

  public List<PlayerRankingItem> handle(int limit) {
    if (limit <= 0 || limit > MAX_LIMIT) {
      throw new IllegalArgumentException("Ranking limit must be between 1 and " + MAX_LIMIT);
    }
    return this.playerProfileQueryRepository.findRanking(limit);
  }
}
