package org.example.idempotency.application.inventory.model;

import java.util.UUID;

public record DecreaseInventoryInput(UUID productId, int quantity) {}
