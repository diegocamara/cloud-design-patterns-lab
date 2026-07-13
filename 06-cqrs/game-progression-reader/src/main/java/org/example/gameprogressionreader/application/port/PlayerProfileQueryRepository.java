package org.example.gameprogressionreader.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.gameprogressionreader.application.query.model.PlayerProfileView;
import org.example.gameprogressionreader.application.query.model.PlayerRankingItem;

public interface PlayerProfileQueryRepository {

  Optional<PlayerProfileView> findById(UUID playerId);

  List<PlayerRankingItem> findRanking(int limit);
}
