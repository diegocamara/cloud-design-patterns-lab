package org.example.idempotency.application.loyaltyaccount.model;

import java.util.UUID;

public record LoyaltyAccountCreditorInput(UUID customerId, long amount) {}
