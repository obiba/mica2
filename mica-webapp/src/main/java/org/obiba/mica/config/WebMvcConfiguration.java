package org.obiba.mica.config;

import org.obiba.mica.web.interceptor.SessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.inject.Inject;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

  private final SessionInterceptor sessionInterceptor;

  @Inject
  public WebMvcConfiguration(SessionInterceptor sessionInterceptor) {
    this.sessionInterceptor = sessionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(sessionInterceptor);
  }
}
