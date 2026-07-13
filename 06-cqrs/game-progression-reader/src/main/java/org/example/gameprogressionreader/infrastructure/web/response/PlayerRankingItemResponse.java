package org.example.gameprogressionreader.infrastructure.web.response;

import java.util.UUID;
import org.example.gameprogressionreader.application.query.model.PlayerRankingItem;

public record PlayerRankingItemResponse(UUID playerId, String nickname, int experience, int level) {

  public static PlayerRankingItemResponse from(PlayerRankingItem player) {
    return new PlayerRankingItemResponse(
        player.playerId(), player.nickname(), player.experience(), player.level());
  }
}
