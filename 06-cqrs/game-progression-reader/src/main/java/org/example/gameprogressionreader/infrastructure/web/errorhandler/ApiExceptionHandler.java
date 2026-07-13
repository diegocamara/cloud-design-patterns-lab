package org.example.gameprogressionreader.infrastructure.web.errorhandler;

import java.net.URI;
import org.example.gameprogressionreader.application.exception.PlayerProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(PlayerProfileNotFoundException.class)
  public ProblemDetail handlePlayerNotFound(PlayerProfileNotFoundException exception) {
    return playerProfileNotFoundProblem(exception.getMessage());
  }

  @ExceptionHandler(
      org.example.gameprogressionreader.infrastructure.processor.exception
          .PlayerProfileNotFoundException.class)
  public ProblemDetail handleProcessorPlayerNotFound(
      org.example.gameprogressionreader.infrastructure.processor.exception
              .PlayerProfileNotFoundException
          exception) {
    return playerProfileNotFoundProblem(exception.getMessage());
  }

  private ProblemDetail playerProfileNotFoundProblem(String detail) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);

    problem.setTitle("Player profile not found");
    problem.setType(URI.create("urn:problem:player-profile-not-found"));

    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());

    problem.setTitle("Invalid request");
    problem.setType(URI.create("urn:problem:invalid-request"));

    return problem;
  }
}
