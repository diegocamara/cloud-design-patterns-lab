package org.example.gameprogressionreader.application.query.model;

import java.util.UUID;

public record PlayerRankingItem(UUID playerId, String nickname, int experience, int level) {}
