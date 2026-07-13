package org.example.gameprogressionreader.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;
import org.example.gameprogressionreader.application.query.handler.GetPlayerProfileQueryHandler;
import org.example.gameprogressionreader.application.query.handler.GetPlayersRankingQueryHandler;
import org.example.gameprogressionreader.infrastructure.web.response.PlayerProfileResponse;
import org.example.gameprogressionreader.infrastructure.web.response.PlayerRankingItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class PlayerQueryController {

  private final GetPlayerProfileQueryHandler getPlayerProfileQueryHandler;
  private final GetPlayersRankingQueryHandler getPlayersRankingQueryHandler;

  public PlayerQueryController(
      GetPlayerProfileQueryHandler getPlayerProfileQueryHandler,
      GetPlayersRankingQueryHandler getPlayersRankingQueryHandler) {
    this.getPlayerProfileQueryHandler = getPlayerProfileQueryHandler;
    this.getPlayersRankingQueryHandler = getPlayersRankingQueryHandler;
  }

  @GetMapping("/players/{playerId}/profile")
  public ResponseEntity<PlayerProfileResponse> getProfile(@PathVariable("playerId") UUID playerId) {
    final var profile = getPlayerProfileQueryHandler.handle(playerId);
    return ResponseEntity.ok(PlayerProfileResponse.from(profile));
  }

  @GetMapping("/ranking/players")
  public ResponseEntity<List<PlayerRankingItemResponse>> getRanking(
      @RequestParam(defaultValue = "100") int limit) {

    final var ranking =
        getPlayersRankingQueryHandler.handle(limit).stream()
            .map(PlayerRankingItemResponse::from)
            .toList();
    return ResponseEntity.ok(ranking);
  }
}
