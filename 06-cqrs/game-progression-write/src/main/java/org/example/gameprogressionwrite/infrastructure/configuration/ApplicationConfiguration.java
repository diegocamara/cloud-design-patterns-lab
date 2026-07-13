package org.example.gameprogressionwrite.infrastructure.configuration;

import java.time.Clock;
import org.example.gameprogressionwrite.application.handler.CompleteStageCommandHandler;
import org.example.gameprogressionwrite.application.handler.CreatePlayerCommandHandler;
import org.example.gameprogressionwrite.application.port.DomainEventsPublisher;
import org.example.gameprogressionwrite.application.port.PlayersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public CreatePlayerCommandHandler createPlayerCommandHandler(
      PlayersRepository playersRepository,
      DomainEventsPublisher domainEventsPublisher,
      Clock clock) {
    return new CreatePlayerCommandHandler(playersRepository, domainEventsPublisher, clock);
  }

  @Bean
  public CompleteStageCommandHandler completeStageCommandHandler(
      PlayersRepository playersRepository,
      DomainEventsPublisher domainEventsPublisher,
      Clock clock) {
    return new CompleteStageCommandHandler(playersRepository, domainEventsPublisher, clock);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
