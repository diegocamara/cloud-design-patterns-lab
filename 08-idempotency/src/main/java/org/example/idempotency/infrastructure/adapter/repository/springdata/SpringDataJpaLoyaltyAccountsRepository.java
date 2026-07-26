package org.example.idempotency.infrastructure.adapter.repository.springdata;

import java.util.UUID;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.LoyaltyAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaLoyaltyAccountsRepository
    extends JpaRepository<LoyaltyAccountEntity, UUID> {}
