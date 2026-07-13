package org.example.gameprogressionreader.infrastructure.web.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.gameprogressionreader.application.query.model.PlayerProfileView;

public record PlayerProfileResponse(
    UUID playerId,
    String nickname,
    int experience,
    int level,
    List<CompletedStageResponse> completedStages,
    Instant createdAt,
    Instant updatedAt) {

  public static PlayerProfileResponse from(PlayerProfileView profile) {
    var stages = profile.completedStages().stream().map(CompletedStageResponse::from).toList();

    return new PlayerProfileResponse(
        profile.playerId(),
        profile.nickname(),
        profile.experience(),
        profile.level(),
        stages,
        profile.createdAt(),
        profile.updatedAt());
  }
}
