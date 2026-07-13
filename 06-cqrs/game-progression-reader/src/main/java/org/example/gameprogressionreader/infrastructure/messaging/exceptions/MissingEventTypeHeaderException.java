package org.example.gameprogressionreader.infrastructure.messaging.exceptions;

public class MissingEventTypeHeaderException extends RuntimeException {
  public MissingEventTypeHeaderException() {
    super("Missing Kafka eventType header");
  }
}
