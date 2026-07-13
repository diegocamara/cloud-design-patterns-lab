package org.example.gameprogressionreader.application.query.model;

import java.time.Instant;

public record CompletedStageView(String stageCode, int xpGained, Instant completedAt) {}
