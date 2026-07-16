package org.example.cacheaside.infrastructure.web.errorhandler;

import java.net.URI;
import org.example.cacheaside.application.exception.ProductNotFoundException;
import org.example.cacheaside.application.exception.ProductWithNameAlreadyExists;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Product not found");
    problem.setType(URI.create("urn:problem:product-not-found"));
    return problem;
  }

  @ExceptionHandler(ProductWithNameAlreadyExists.class)
  public ProblemDetail handleProductWithNameAlreadyExists(
      ProductWithNameAlreadyExists exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    problem.setTitle("Product already exists");
    problem.setType(URI.create("urn:problem:product-already-exists"));
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
    var detail =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
            .orElse("Invalid request body");

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Invalid request");
    problem.setType(URI.create("urn:problem:invalid-request"));
    return problem;
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class
  })
  public ProblemDetail handleInvalidRequest(Exception exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid request");
    problem.setType(URI.create("urn:problem:invalid-request"));
    return problem;
  }
}
