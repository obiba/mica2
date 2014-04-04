package org.obiba.mica.config;

import org.obiba.shiro.web.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSecurityConfigurer {

  @Bean
  public AuthenticationFilter authenticationFilter() {
    AuthenticationFilter filter = new AuthenticationFilter();
    filter.setHeaderCredentials("WWW-Authenticate");
    filter.setSessionIdCookieName("micasid");
    filter.setRequestIdCookieName("micarid");
    return filter;
  }

}
