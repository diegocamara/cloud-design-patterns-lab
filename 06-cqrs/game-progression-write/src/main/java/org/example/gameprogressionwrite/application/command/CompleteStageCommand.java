package org.example.gameprogressionwrite.application.command;

import java.util.UUID;

public record CompleteStageCommand(UUID playerId, String stageCode, int xpGained) {}
