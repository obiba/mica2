package org.obiba.mica.config;

import org.obiba.mica.security.SecurityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(CacheConfiguration.class)
public class SecurityConfiguration {

  @Bean
  public SecurityManagerFactory securityManagerFactory() {
    return new SecurityManagerFactory();
  }

}
