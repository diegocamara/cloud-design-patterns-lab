package org.example.gameprogressionwrite.domain.event;

import java.time.Instant;
import java.util.UUID;
import org.example.gameprogressionwrite.domain.model.PlayerId;

public record PlayerCreated(UUID eventId, PlayerId playerId, String nickname, Instant occurredAt)
    implements DomainEvent {

  @Override
  public UUID aggregateId() {
    return playerId.value();
  }

  @Override
  public String eventType() {
    return "PlayerCreated";
  }
}
