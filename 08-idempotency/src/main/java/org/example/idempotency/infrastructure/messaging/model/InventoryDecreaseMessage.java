package org.example.idempotency.infrastructure.messaging.model;

import java.util.UUID;

public record InventoryDecreaseMessage(String idempotencyKey, UUID productId, int quantity) {}
