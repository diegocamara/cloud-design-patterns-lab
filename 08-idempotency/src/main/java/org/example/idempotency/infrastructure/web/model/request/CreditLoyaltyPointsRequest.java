package org.example.idempotency.infrastructure.web.model.request;

import jakarta.validation.constraints.Positive;

public record CreditLoyaltyPointsRequest(@Positive long amount) {}
