package org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "processed_events")
public class ProcessedEventDocument {

  @Id private UUID eventId;

  private Instant processedAt;

  protected ProcessedEventDocument() {}

  public ProcessedEventDocument(UUID eventId, Instant processedAt) {
    this.eventId = eventId;
    this.processedAt = processedAt;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
