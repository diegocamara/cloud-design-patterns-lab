package org.example.idempotency.domain.task.model;

import java.util.UUID;

public class Task {
  private final Id id;
  private final Title title;

  private Task(Id id, Title title) {
    this.id = id;
    this.title = title;
  }

  public static Task of(UUID id, String title) {
    return new Task(new Id(id), new Title(title));
  }

  public Id getId() {
    return id;
  }

  public Title getTitle() {
    return title;
  }
}
