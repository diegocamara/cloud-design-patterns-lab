package org.example.gameprogressionwrite.infrastructure.web.errorhandler;

import java.net.URI;
import org.example.gameprogressionwrite.application.exception.PlayerNotFoundException;
import org.example.gameprogressionwrite.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final String STAGE_ALREADY_COMPLETED = "Stage already completed by this player";

  @ExceptionHandler(PlayerNotFoundException.class)
  public ProblemDetail handlePlayerNotFound(PlayerNotFoundException exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Player not found");
    problem.setType(URI.create("urn:problem:player-not-found"));
    return problem;
  }

  @ExceptionHandler(DomainException.class)
  public ProblemDetail handleDomainException(DomainException exception) {
    if (STAGE_ALREADY_COMPLETED.equals(exception.getMessage())) {
      var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
      problem.setTitle("Stage already completed");
      problem.setType(URI.create("urn:problem:stage-already-completed"));
      return problem;
    }

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid command");
    problem.setType(URI.create("urn:problem:invalid-command"));
    return problem;
  }
}
