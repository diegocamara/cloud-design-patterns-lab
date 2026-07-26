package org.example.idempotency.infrastructure.web.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.example.idempotency.application.loyaltyaccount.model.LoyaltyAccountCreditorInput;
import org.example.idempotency.application.loyaltyaccount.usercase.LoyaltyAccountCreditor;
import org.example.idempotency.infrastructure.web.model.request.CreditLoyaltyPointsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loyalty-accounts")
public class LoyaltyPointsController {

  private final LoyaltyAccountCreditor loyaltyAccountCreditor;

  public LoyaltyPointsController(LoyaltyAccountCreditor loyaltyAccountCreditor) {
    this.loyaltyAccountCreditor = loyaltyAccountCreditor;
  }

  @PostMapping("/customers/{customerId}/loyalty-points")
  public ResponseEntity<Void> credit(
      @NotBlank @RequestHeader("Idempotency-Key") String idempotencyKey,
      @NotNull @PathVariable("customerId") UUID customerId,
      @NotNull @RequestBody CreditLoyaltyPointsRequest request) {
    this.loyaltyAccountCreditor.credit(
        new LoyaltyAccountCreditorInput(customerId, request.amount()));
    return ResponseEntity.noContent().build();
  }
}
