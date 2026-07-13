package org.example.gameprogressionreader.infrastructure.processor;

import org.example.gameprogression.contracts.PlayerCreatedMessage;
import org.example.gameprogression.contracts.StageCompletedMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GameProgressionProjectionProcessor {

  private final PlayerCreatedProjectionProcessor playerCreatedProjectionProcessor;
  private final StageCompletedProjectionProcessor stageCompletedProjectionProcessor;

  public GameProgressionProjectionProcessor(
      PlayerCreatedProjectionProcessor playerCreatedProjectionProcessor,
      StageCompletedProjectionProcessor stageCompletedProjectionProcessor) {
    this.playerCreatedProjectionProcessor = playerCreatedProjectionProcessor;
    this.stageCompletedProjectionProcessor = stageCompletedProjectionProcessor;
  }

  @Transactional
  public void process(Object event) {

    switch (event) {
      case PlayerCreatedMessage playerCreatedMessage ->
          this.playerCreatedProjectionProcessor.process(playerCreatedMessage);
      case StageCompletedMessage stageCompletedMessage ->
          this.stageCompletedProjectionProcessor.process(stageCompletedMessage);
      default -> throw new IllegalArgumentException("Unknown event type: " + event);
    }
  }
}
