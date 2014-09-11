package org.obiba.mica.config;

import org.obiba.mica.core.LoggingAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

  @Bean
  @Profile(Profiles.DEV)
  public LoggingAspect loggingAspect() {
    return new LoggingAspect();
  }
}
