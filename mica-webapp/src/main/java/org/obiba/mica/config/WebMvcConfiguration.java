package org.obiba.mica.config;

import com.google.common.eventbus.Subscribe;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.web.interceptor.MicaConfigInterceptor;
import org.obiba.mica.web.interceptor.SessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.inject.Inject;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final SessionInterceptor sessionInterceptor;

  private final MicaConfigInterceptor micaConfigInterceptor;

  @Inject
  public WebMvcConfiguration(SessionInterceptor sessionInterceptor, MicaConfigInterceptor micaConfigInterceptor) {
    this.sessionInterceptor = sessionInterceptor;
    this.micaConfigInterceptor = micaConfigInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(sessionInterceptor);
    registry.addInterceptor(micaConfigInterceptor);
  }

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    micaConfigInterceptor.evict();
  }
}
