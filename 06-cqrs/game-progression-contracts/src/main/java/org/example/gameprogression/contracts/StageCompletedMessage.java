package org.example.gameprogression.contracts;

import java.time.Instant;
import java.util.UUID;

public record StageCompletedMessage(
    UUID eventId,
    UUID playerId,
    String stageCode,
    int xpGained,
    int totalExperience,
    int level,
    Instant occurredAt) {}
