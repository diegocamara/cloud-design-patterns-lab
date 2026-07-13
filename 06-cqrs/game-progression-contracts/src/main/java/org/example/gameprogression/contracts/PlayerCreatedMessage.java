package org.example.gameprogression.contracts;

import java.time.Instant;
import java.util.UUID;

public record PlayerCreatedMessage(
    UUID eventId, UUID playerId, String nickname, Instant occurredAt) {}
