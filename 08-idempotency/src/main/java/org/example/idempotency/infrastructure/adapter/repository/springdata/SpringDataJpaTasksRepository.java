package org.example.idempotency.infrastructure.adapter.repository.springdata;

import java.util.UUID;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaTasksRepository extends JpaRepository<TaskEntity, UUID> {}
