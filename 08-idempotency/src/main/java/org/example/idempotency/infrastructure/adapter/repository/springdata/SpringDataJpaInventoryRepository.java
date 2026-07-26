package org.example.idempotency.infrastructure.adapter.repository.springdata;

import java.util.UUID;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaInventoryRepository extends JpaRepository<InventoryEntity, UUID> {}
