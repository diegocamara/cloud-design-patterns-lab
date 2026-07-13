package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "aggregate_id", nullable = false, updatable = false)
  private UUID aggregateId;

  @Column(name = "aggregate_type", nullable = false, updatable = false, length = 100)
  private String aggregateType;

  @Column(name = "event_type", nullable = false, updatable = false, length = 100)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private OutboxEventStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  protected OutboxEventEntity() {}

  private OutboxEventEntity(
      UUID id,
      UUID aggregateId,
      String aggregateType,
      String eventType,
      String payload,
      OutboxEventStatus status,
      int attempts,
      String lastError,
      Instant createdAt,
      Instant publishedAt) {
    this.id = Objects.requireNonNull(id);
    this.aggregateId = Objects.requireNonNull(aggregateId);
    this.aggregateType = Objects.requireNonNull(aggregateType);
    this.eventType = Objects.requireNonNull(eventType);
    this.payload = Objects.requireNonNull(payload);
    this.status = Objects.requireNonNull(status);
    this.attempts = attempts;
    this.lastError = lastError;
    this.createdAt = Objects.requireNonNull(createdAt);
    this.publishedAt = publishedAt;
  }

  public static OutboxEventEntity pending(
      UUID eventId,
      UUID aggregateId,
      String aggregateType,
      String eventType,
      String payload,
      Instant createdAt) {
    return new OutboxEventEntity(
        eventId,
        aggregateId,
        aggregateType,
        eventType,
        payload,
        OutboxEventStatus.PENDING,
        0,
        null,
        createdAt,
        null);
  }

  public void markAsPublished(Instant publishedAt) {
    this.status = OutboxEventStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(publishedAt);
    this.lastError = null;
  }

  public void registerFailure(String error) {
    this.attempts++;
    this.status = OutboxEventStatus.FAILED;
    this.lastError = error;
  }

  public void scheduleRetry() {
    this.status = OutboxEventStatus.PENDING;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }
}
