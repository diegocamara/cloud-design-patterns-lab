package org.example.gameprogressionreader.infrastructure.service;

import java.time.Clock;
import java.util.UUID;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.SpringDataMongoProcessedEventsRepository;
import org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model.ProcessedEventDocument;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventRegister {

  private final SpringDataMongoProcessedEventsRepository springDataMongoProcessedEventsRepository;
  private final Clock clock;

  public ProcessedEventRegister(
      final SpringDataMongoProcessedEventsRepository springDataMongoProcessedEventsRepository,
      Clock clock) {
    this.springDataMongoProcessedEventsRepository = springDataMongoProcessedEventsRepository;
    this.clock = clock;
  }

  public void register(UUID eventId) {
    this.springDataMongoProcessedEventsRepository.insert(
        new ProcessedEventDocument(eventId, this.clock.instant()));
  }
}
