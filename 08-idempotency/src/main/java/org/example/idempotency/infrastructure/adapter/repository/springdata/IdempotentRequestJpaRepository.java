package org.example.idempotency.infrastructure.adapter.repository.springdata;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.IdempotencyStatus;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.IdempotentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotentRequestJpaRepository
    extends JpaRepository<IdempotentRequestEntity, UUID> {

  @Modifying
  @Query(
      value =
          """
            INSERT INTO idempotent_requests (
                id,
                operation_name,
                idempotency_key,
                request_hash,
                status,
                created_at,
                expires_at
            )
            VALUES (
                :id,
                :operationName,
                :idempotencyKey,
                :requestHash,
                'PROCESSING',
                :createdAt,
                :expiresAt
            )
            ON CONFLICT (
                operation_name,
                idempotency_key
            )
            DO UPDATE
               SET id = EXCLUDED.id,
                   request_hash = EXCLUDED.request_hash,
                   status = 'PROCESSING',
                   http_status = NULL,
                   response_body = NULL,
                   created_at = EXCLUDED.created_at,
                   completed_at = NULL,
                   expires_at = EXCLUDED.expires_at
             WHERE idempotent_requests.expires_at <= EXCLUDED.created_at
            """,
      nativeQuery = true)
  int tryInsert(
      @Param("id") UUID id,
      @Param("operationName") String operationName,
      @Param("idempotencyKey") String idempotencyKey,
      @Param("requestHash") String requestHash,
      @Param("createdAt") OffsetDateTime createdAt,
      @Param("expiresAt") OffsetDateTime expiresAt);

  @Modifying
  @Query(
      """
        UPDATE IdempotentRequestEntity request
           SET request.status = :status,
               request.httpStatus = :httpStatus,
               request.responseBody = :responseBody,
               request.completedAt = :completedAt
         WHERE request.id = :id
        """)
  int complete(
      @Param("id") UUID id,
      @Param("status") IdempotencyStatus status,
      @Param("httpStatus") int httpStatus,
      @Param("responseBody") Map<String, Object> responseBody,
      @Param("completedAt") OffsetDateTime completedAt);

  Optional<IdempotentRequestEntity> findByOperationNameAndIdempotencyKey(
      String operationName, String idempotencyKey);
}
