package org.example.gameprogressionreader.infrastructure.repository.springdatamongo.model;

import java.time.Instant;

public record CompletedStageDocument(String stageCode, int xpGained, Instant completedAt) {}
