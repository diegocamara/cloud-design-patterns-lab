package org.example.gameprogressionwrite.application.handler;

import java.time.Clock;
import java.util.Objects;
import org.example.gameprogressionwrite.application.command.CompleteStageCommand;
import org.example.gameprogressionwrite.application.exception.PlayerNotFoundException;
import org.example.gameprogressionwrite.application.port.DomainEventsPublisher;
import org.example.gameprogressionwrite.application.port.PlayersRepository;
import org.example.gameprogressionwrite.domain.model.PlayerId;
import org.example.gameprogressionwrite.domain.model.StageCode;

public final class CompleteStageCommandHandler {

  private final PlayersRepository playersRepository;
  private final DomainEventsPublisher domainEventsPublisher;
  private final Clock clock;

  public CompleteStageCommandHandler(
      PlayersRepository playersRepository,
      DomainEventsPublisher domainEventsPublisher,
      Clock clock) {
    this.playersRepository = playersRepository;
    this.domainEventsPublisher = domainEventsPublisher;
    this.clock = clock;
  }

  public void handle(CompleteStageCommand command) {
    Objects.requireNonNull(command, "command cannot be null");

    final var playerId = new PlayerId(command.playerId());

    final var player =
        this.playersRepository
            .findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

    player.completeStage(new StageCode(command.stageCode()), command.xpGained(), clock.instant());

    this.playersRepository.save(player);

    this.domainEventsPublisher.publish(player.pullEvents());
  }
}
