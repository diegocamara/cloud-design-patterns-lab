package org.example.gameprogressionwrite.infrastructure.web.controller;

import java.net.URI;
import java.util.UUID;
import org.example.gameprogressionwrite.application.command.CompleteStageCommand;
import org.example.gameprogressionwrite.application.command.CreatePlayerCommand;
import org.example.gameprogressionwrite.application.handler.CompleteStageCommandHandler;
import org.example.gameprogressionwrite.application.handler.CreatePlayerCommandHandler;
import org.example.gameprogressionwrite.infrastructure.web.request.CompleteStageRequest;
import org.example.gameprogressionwrite.infrastructure.web.request.CreatePlayerRequest;
import org.example.gameprogressionwrite.infrastructure.web.response.CreatePlayerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players")
public class PlayerCommandController {

  private CreatePlayerCommandHandler createPlayerCommandHandler;
  private CompleteStageCommandHandler completeStageCommandHandler;
  private TransactionTemplate transactionTemplate;

  public PlayerCommandController(
      CreatePlayerCommandHandler createPlayerCommandHandler,
      CompleteStageCommandHandler completeStageCommandHandler,
      TransactionTemplate transactionTemplate) {
    this.createPlayerCommandHandler = createPlayerCommandHandler;
    this.completeStageCommandHandler = completeStageCommandHandler;
    this.transactionTemplate = transactionTemplate;
  }

  @PostMapping
  public ResponseEntity<CreatePlayerResponse> createPlayer(
      @RequestBody CreatePlayerRequest request) {

    final var command = new CreatePlayerCommand(request.nickname());

    final var playerId =
        this.transactionTemplate.execute(status -> this.createPlayerCommandHandler.handle(command));

    final var location = URI.create("/players/" + playerId.value());

    return ResponseEntity.created(location).body(new CreatePlayerResponse(playerId.value()));
  }

  @PostMapping("/{playerId}/stages/{stageCode}/completion")
  public ResponseEntity<Void> completeStage(
      @PathVariable UUID playerId,
      @PathVariable String stageCode,
      @RequestBody CompleteStageRequest request) {

    final var command = new CompleteStageCommand(playerId, stageCode, request.xpGained());

    this.transactionTemplate.executeWithoutResult(
        status -> this.completeStageCommandHandler.handle(command));

    return ResponseEntity.noContent().build();
  }
}
