package org.example.idempotency.application.loyaltyaccount.port;

import java.util.Optional;
import java.util.UUID;
import org.example.idempotency.domain.loyaltyaccount.model.LoyaltyAccount;

public interface LoyaltyAccountsRepository {
  void save(LoyaltyAccount loyaltyAccount);

  Optional<LoyaltyAccount> findByCustomerId(UUID customerId);
}
