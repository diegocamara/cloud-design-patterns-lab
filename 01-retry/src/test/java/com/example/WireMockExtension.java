package com.example;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.*;

public class WireMockExtension
    implements BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver {

  private final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    this.wireMockServer.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    this.wireMockServer.resetAll();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    this.wireMockServer.stop();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().isAssignableFrom(WireMockServer.class);
  }

  @Override
  public @Nullable Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return this.wireMockServer;
  }
}
