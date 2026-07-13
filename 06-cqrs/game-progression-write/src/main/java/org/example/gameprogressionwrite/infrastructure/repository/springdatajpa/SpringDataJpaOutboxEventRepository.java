package org.example.gameprogressionwrite.infrastructure.repository.springdatajpa;

import java.util.List;
import java.util.UUID;
import org.example.gameprogressionwrite.infrastructure.repository.springdatajpa.model.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataJpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Query(
      value =
          """
                 select *
                 from outbox_events
                 where status in ('PENDING', 'FAILED')
                 and attempts < :maxAttempts
                 order by created_at
                 limit :batchSize
                 for update skip locked
                 """,
      nativeQuery = true)
  List<OutboxEventEntity> findNextBatch(
      @Param("batchSize") int batchSize, @Param("maxAttempts") int maxAttempts);
}
