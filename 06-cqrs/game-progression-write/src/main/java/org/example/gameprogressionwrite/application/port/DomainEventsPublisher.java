package org.example.gameprogressionwrite.application.port;

import java.util.Collection;
import org.example.gameprogressionwrite.domain.event.DomainEvent;

public interface DomainEventsPublisher {
  void publish(Collection<DomainEvent> events);
}
