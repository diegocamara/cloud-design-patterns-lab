package org.example.gameprogressionreader.infrastructure.processor;

import org.example.gameprogression.contracts.StageCompletedMessage;
import org.example.gameprogressionreader.infrastructure.processor.exception.PlayerProfileProjectionNotFoundException;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.SpringDataMongoPlayerProfilesRepository;
import org.example.gameprogressionreader.infrastructure.service.ProcessedEventRegister;
import org.springframework.stereotype.Component;

@Component
public class StageCompletedProjectionProcessor {

  private final ProcessedEventRegister processedEventRegister;
  private final SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository;

  public StageCompletedProjectionProcessor(
      ProcessedEventRegister processedEventRegister,
      SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository) {
    this.processedEventRegister = processedEventRegister;
    this.springDataMongoPlayerProfilesRepository = springDataMongoPlayerProfilesRepository;
  }

  public void process(StageCompletedMessage event) {
    this.processedEventRegister.register(event.eventId());

    final var playerProfileDocument =
        this.springDataMongoPlayerProfilesRepository
            .findById(event.playerId())
            .orElseThrow(() -> new PlayerProfileProjectionNotFoundException(event.playerId()));

    playerProfileDocument.completeStage(
        event.stageCode(),
        event.xpGained(),
        event.totalExperience(),
        event.level(),
        event.occurredAt());

    this.springDataMongoPlayerProfilesRepository.save(playerProfileDocument);
  }
}
