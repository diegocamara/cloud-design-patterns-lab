package org.example.gameprogressionwrite.domain.event;

import java.time.Instant;
import java.util.UUID;
import org.example.gameprogressionwrite.domain.model.PlayerId;

public record StageCompleted(
    UUID eventId,
    PlayerId playerId,
    String stageCode,
    int xpGained,
    int totalExperience,
    int level,
    Instant occurredAt)
    implements DomainEvent {

  @Override
  public UUID aggregateId() {
    return playerId.value();
  }

  @Override
  public String eventType() {
    return "StageCompleted";
  }
}
