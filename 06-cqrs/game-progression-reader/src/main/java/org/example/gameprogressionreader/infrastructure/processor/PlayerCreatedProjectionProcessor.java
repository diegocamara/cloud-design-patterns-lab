package org.example.gameprogressionreader.infrastructure.processor;

import org.example.PlayerCreatedMessage;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.SpringDataMongoPlayerProfilesRepository;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model.PlayerProfileDocument;
import org.example.gameprogressionreader.infrastructure.service.ProcessedEventRegister;
import org.springframework.stereotype.Component;

@Component
public class PlayerCreatedProjectionProcessor {

  private final ProcessedEventRegister processedEventRegister;
  private final SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository;

  public PlayerCreatedProjectionProcessor(
      ProcessedEventRegister processedEventRegister,
      SpringDataMongoPlayerProfilesRepository springDataMongoPlayerProfilesRepository) {
    this.processedEventRegister = processedEventRegister;
    this.springDataMongoPlayerProfilesRepository = springDataMongoPlayerProfilesRepository;
  }

  public void process(PlayerCreatedMessage event) {
    this.processedEventRegister.register(event.eventId());

    final var playerProfileDocument =
        new PlayerProfileDocument(
            event.playerId(), event.nickname(), 0, 1, event.occurredAt(), event.occurredAt());

    this.springDataMongoPlayerProfilesRepository.save(playerProfileDocument);
  }
}
