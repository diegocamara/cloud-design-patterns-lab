package org.example.idempotency.application.loyaltyaccount.usercase;

import java.util.Objects;
import org.example.idempotency.application.loyaltyaccount.model.LoyaltyAccountCreditorInput;
import org.example.idempotency.application.loyaltyaccount.port.LoyaltyAccountsRepository;

public final class LoyaltyAccountCreditor {

  private final LoyaltyAccountsRepository loyaltyAccountsRepository;

  public LoyaltyAccountCreditor(LoyaltyAccountsRepository loyaltyAccountsRepository) {
    this.loyaltyAccountsRepository = loyaltyAccountsRepository;
  }

  public void credit(LoyaltyAccountCreditorInput input) {
    Objects.requireNonNull(input, "LoyaltyAccountCreditorInput cannot be null");
    final var loyaltyAccount =
        this.loyaltyAccountsRepository.findByCustomerId(input.customerId()).orElseThrow();

    loyaltyAccount.credit(input.amount());

    this.loyaltyAccountsRepository.save(loyaltyAccount);
  }
}
