package org.example.gameprogressionwrite.infrastructure.eventpublisher;

import java.util.Collection;
import org.example.PlayerCreatedMessage;
import org.example.StageCompletedMessage;
import org.example.gameprogressionwrite.application.port.DomainEventsPublisher;
import org.example.gameprogressionwrite.domain.event.DomainEvent;
import org.example.gameprogressionwrite.domain.event.PlayerCreated;
import org.example.gameprogressionwrite.domain.event.StageCompleted;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.SpringDataJpaOutboxEventRepository;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model.OutboxEventEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxTableEventPublisher implements DomainEventsPublisher {

  public static final String AGGREGATE_TYPE = "Player";
  private final SpringDataJpaOutboxEventRepository springDataJpaOutboxEventRepository;
  private final ObjectMapper objectMapper;

  public OutboxTableEventPublisher(
      SpringDataJpaOutboxEventRepository springDataJpaOutboxEventRepository,
      ObjectMapper objectMapper) {
    this.springDataJpaOutboxEventRepository = springDataJpaOutboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(Collection<DomainEvent> events) {
    final var entities = events.stream().map(this::toPendingEntity).toList();
    this.springDataJpaOutboxEventRepository.saveAll(entities);
  }

  private OutboxEventEntity toPendingEntity(DomainEvent event) {
    final var message = toMessage(event);

    return OutboxEventEntity.pending(
        event.eventId(),
        event.aggregateId(),
        AGGREGATE_TYPE,
        message.getClass().getSimpleName(),
        serialize(message),
        event.occurredAt());
  }

  private Object toMessage(DomainEvent event) {
    return switch (event) {
      case PlayerCreated playerCreated ->
          new PlayerCreatedMessage(
              playerCreated.eventId(),
              playerCreated.aggregateId(),
              playerCreated.nickname(),
              playerCreated.occurredAt());
      case StageCompleted stageCompleted ->
          new StageCompletedMessage(
              stageCompleted.eventId(),
              stageCompleted.aggregateId(),
              stageCompleted.stageCode(),
              stageCompleted.xpGained(),
              stageCompleted.totalExperience(),
              stageCompleted.level(),
              stageCompleted.occurredAt());
    };
  }

  private String serialize(Object message) {
    try {
      return this.objectMapper.writeValueAsString(message);
    } catch (JacksonException exception) {
      throw new RuntimeException("Could not serialize integration message", exception);
    }
  }
}
