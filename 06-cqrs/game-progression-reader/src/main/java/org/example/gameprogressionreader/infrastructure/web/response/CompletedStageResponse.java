package org.example.gameprogressionreader.infrastructure.web.response;

import java.time.Instant;
import org.example.gameprogressionreader.application.query.model.CompletedStageView;

public record CompletedStageResponse(String stageCode, int xpGained, Instant completedAt) {

  public static CompletedStageResponse from(CompletedStageView stage) {
    return new CompletedStageResponse(stage.stageCode(), stage.xpGained(), stage.completedAt());
  }
}
