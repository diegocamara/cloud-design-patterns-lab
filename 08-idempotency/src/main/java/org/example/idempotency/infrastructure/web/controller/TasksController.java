package org.example.idempotency.infrastructure.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.idempotency.application.task.model.TaskCreatorInput;
import org.example.idempotency.application.task.usercase.TaskCreator;
import org.example.idempotency.infrastructure.idempotency.IdempotencyExecutor;
import org.example.idempotency.infrastructure.web.model.request.CreateTaskRequest;
import org.example.idempotency.infrastructure.web.model.response.CreateTaskResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TasksController {

  private final String OPERATION_NAME = "CREATE_TASK";

  private final TaskCreator taskCreator;
  private final IdempotencyExecutor idempotencyExecutor;

  public TasksController(TaskCreator taskCreator, IdempotencyExecutor idempotencyExecutor) {
    this.taskCreator = taskCreator;
    this.idempotencyExecutor = idempotencyExecutor;
  }

  @PostMapping
  public ResponseEntity<CreateTaskResponse> create(
      @NotBlank @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @NotNull @RequestBody CreateTaskRequest request) {

    final var input = new TaskCreatorInput(request.title());
    final var result =
        this.idempotencyExecutor.execute(
            OPERATION_NAME,
            idempotencyKey,
            input,
            HttpStatus.CREATED.value(),
            CreateTaskResponse.class,
            () -> {
              final var task = this.taskCreator.create(input);
              return new CreateTaskResponse(task.getId().value(), task.getTitle().value());
            });

    return ResponseEntity.status(result.httpStatus())
        .header("Idempotent-Replayed", Boolean.toString(result.replayed()))
        .body(result.body());
  }
}
