package org.obiba.mica.config;

import com.google.common.eventbus.Subscribe;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.web.interceptor.MicaConfigInterceptor;
import org.obiba.mica.web.interceptor.SessionInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.inject.Inject;
import java.util.List;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final SessionInterceptor sessionInterceptor;

  private final MicaConfigInterceptor micaConfigInterceptor;

  @Value("#{'${cors.allowed-origins:}'.split(',')}")
  private List<String> allowedOrigins;

  @Inject
  public WebMvcConfiguration(SessionInterceptor sessionInterceptor, MicaConfigInterceptor micaConfigInterceptor) {
    this.sessionInterceptor = sessionInterceptor;
    this.micaConfigInterceptor = micaConfigInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    if (allowedOrigins != null && !allowedOrigins.isEmpty() && !allowedOrigins.get(0).isBlank()) {
      registry.addMapping("/**")
        .allowedOrigins(allowedOrigins.toArray(new String[0]))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(1800);
    }
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
