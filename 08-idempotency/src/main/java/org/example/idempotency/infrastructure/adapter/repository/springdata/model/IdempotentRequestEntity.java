package org.example.idempotency.infrastructure.adapter.repository.springdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "idempotent_requests",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_idempotent_requests",
          columnNames = {"operation_name", "idempotency_key"})
    })
public class IdempotentRequestEntity {

  @Id private UUID id;

  @Column(name = "operation_name", nullable = false, length = 100)
  private String operationName;

  @Column(name = "idempotency_key", nullable = false, length = 255)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private IdempotencyStatus status;

  @Column(name = "http_status")
  private Integer httpStatus;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_body", columnDefinition = "jsonb")
  private Map<String, Object> responseBody;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  protected IdempotentRequestEntity() {}

  private IdempotentRequestEntity(
      UUID id,
      String operationName,
      String idempotencyKey,
      String requestHash,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt) {
    this.id = id;
    this.operationName = operationName;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.status = IdempotencyStatus.PROCESSING;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public static IdempotentRequestEntity processing(
      String operationName, String idempotencyKey, String requestHash, OffsetDateTime expiresAt) {
    return new IdempotentRequestEntity(
        UUID.randomUUID(),
        operationName,
        idempotencyKey,
        requestHash,
        OffsetDateTime.now(),
        expiresAt);
  }

  public void complete(int httpStatus, Map<String, Object> responseBody) {
    if (status == IdempotencyStatus.COMPLETED) {
      throw new IllegalStateException("A requisição idempotente já foi concluída");
    }

    this.status = IdempotencyStatus.COMPLETED;
    this.httpStatus = httpStatus;
    this.responseBody = responseBody;
    this.completedAt = OffsetDateTime.now();
  }

  public boolean isCompleted() {
    return status == IdempotencyStatus.COMPLETED;
  }

  public UUID getId() {
    return id;
  }

  public String getOperationName() {
    return operationName;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public IdempotencyStatus getStatus() {
    return status;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public Map<String, Object> getResponseBody() {
    return responseBody;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }
}
