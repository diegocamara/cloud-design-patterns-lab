package org.example.gameprogressionreader.infrastructure.configuration;

import java.time.Clock;
import org.example.gameprogressionreader.application.port.PlayerProfileQueryRepository;
import org.example.gameprogressionreader.application.query.handler.GetPlayerProfileQueryHandler;
import org.example.gameprogressionreader.application.query.handler.GetPlayersRankingQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public GetPlayerProfileQueryHandler getPlayerProfileQueryHandler(
      PlayerProfileQueryRepository playerProfileQueryRepository) {
    return new GetPlayerProfileQueryHandler(playerProfileQueryRepository);
  }

  @Bean
  public GetPlayersRankingQueryHandler getPlayersRankingQueryHandler(
      PlayerProfileQueryRepository playerProfileQueryRepository) {
    return new GetPlayersRankingQueryHandler(playerProfileQueryRepository);
  }
}
