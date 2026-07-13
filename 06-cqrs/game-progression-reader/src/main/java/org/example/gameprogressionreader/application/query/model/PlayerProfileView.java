package org.example.gameprogressionreader.application.query.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlayerProfileView(
    UUID playerId,
    String nickname,
    int experience,
    int level,
    List<CompletedStageView> completedStages,
    Instant createdAt,
    Instant updatedAt) {

  public PlayerProfileView {
    completedStages = List.copyOf(completedStages);
  }
}
