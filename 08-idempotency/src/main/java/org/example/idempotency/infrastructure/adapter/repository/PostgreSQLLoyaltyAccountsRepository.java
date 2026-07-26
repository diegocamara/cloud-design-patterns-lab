package org.example.idempotency.infrastructure.adapter.repository;

import java.util.Optional;
import java.util.UUID;
import org.example.idempotency.application.loyaltyaccount.port.LoyaltyAccountsRepository;
import org.example.idempotency.domain.loyaltyaccount.model.LoyaltyAccount;
import org.example.idempotency.infrastructure.adapter.repository.springdata.SpringDataJpaLoyaltyAccountsRepository;
import org.example.idempotency.infrastructure.adapter.repository.springdata.model.LoyaltyAccountEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PostgreSQLLoyaltyAccountsRepository implements LoyaltyAccountsRepository {

  private final SpringDataJpaLoyaltyAccountsRepository springDataJpaLoyaltyAccountsRepository;

  public PostgreSQLLoyaltyAccountsRepository(
      SpringDataJpaLoyaltyAccountsRepository springDataJpaLoyaltyAccountsRepository) {
    this.springDataJpaLoyaltyAccountsRepository = springDataJpaLoyaltyAccountsRepository;
  }

  @Override
  public void save(LoyaltyAccount loyaltyAccount) {
    this.springDataJpaLoyaltyAccountsRepository.save(new LoyaltyAccountEntity(loyaltyAccount));
  }

  @Override
  public Optional<LoyaltyAccount> findByCustomerId(UUID customerId) {
    return this.springDataJpaLoyaltyAccountsRepository
        .findById(customerId)
        .map(this::toLoyaltyAccount);
  }

  private LoyaltyAccount toLoyaltyAccount(LoyaltyAccountEntity entity) {
    return LoyaltyAccount.of(entity.getCustomerId(), entity.getPoints(), entity.getUpdatedAt());
  }
}
