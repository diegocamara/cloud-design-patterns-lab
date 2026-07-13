package org.example.gameprogressionwrite.application.handler;

import java.time.Clock;
import java.util.Objects;
import org.example.gameprogressionwrite.application.command.CreatePlayerCommand;
import org.example.gameprogressionwrite.application.port.DomainEventsPublisher;
import org.example.gameprogressionwrite.application.port.PlayersRepository;
import org.example.gameprogressionwrite.domain.model.Nickname;
import org.example.gameprogressionwrite.domain.model.Player;
import org.example.gameprogressionwrite.domain.model.PlayerId;

public final class CreatePlayerCommandHandler {

  private final PlayersRepository playersRepository;
  private final DomainEventsPublisher domainEventsPublisher;
  private final Clock clock;

  public CreatePlayerCommandHandler(
      PlayersRepository playersRepository,
      DomainEventsPublisher domainEventsPublisher,
      Clock clock) {
    this.playersRepository = playersRepository;
    this.domainEventsPublisher = domainEventsPublisher;
    this.clock = clock;
  }

  public PlayerId handle(CreatePlayerCommand command) {
    Objects.requireNonNull(command, "command cannot be null");

    final var player =
        Player.create(PlayerId.newId(), new Nickname(command.nickname()), clock.instant());

    this.playersRepository.save(player);

    this.domainEventsPublisher.publish(player.pullEvents());

    return player.id();
  }
}
