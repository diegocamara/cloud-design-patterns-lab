package org.example.gameprogressionwrite.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent permits PlayerCreated, StageCompleted {

  UUID eventId();

  UUID aggregateId();

  String eventType();

  Instant occurredAt();
}
