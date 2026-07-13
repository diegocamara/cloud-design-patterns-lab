package org.example.gameprogressionwrite.application.port;

import java.util.Optional;
import org.example.gameprogressionwrite.domain.model.Player;
import org.example.gameprogressionwrite.domain.model.PlayerId;

public interface PlayersRepository {
  void save(Player player);

  Optional<Player> findById(PlayerId playerId);
}
