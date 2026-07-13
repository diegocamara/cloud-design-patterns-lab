package org.example.gameprogressionwrite.domain.model;

import java.time.Instant;
import java.util.Objects;
import org.example.gameprogressionwrite.domain.exception.DomainException;

public record CompletedStage(StageCode stageCode, int xpGained, Instant completedAt) {

  public CompletedStage {
    Objects.requireNonNull(stageCode, "Stage code cannot be null");
    Objects.requireNonNull(completedAt, "Completed at cannot be null");

    if (xpGained <= 0) {
      throw new DomainException("XP gained must be greater than zero");
    }
  }
}
