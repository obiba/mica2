package org.obiba.mica.config;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.obiba.mica.web.rest.security.AuthenticationInterceptor;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath(JerseyConfiguration.WS_ROOT)
public class JerseyConfiguration extends ResourceConfig {

  public static final String WS_ROOT = "/ws";

  public JerseyConfiguration() {
    register(RequestContextFilter.class);
    packages("org.obiba.mica", "org.obiba.jersey", "com.fasterxml.jackson");
    register(LoggingFilter.class);
    register(AuthenticationInterceptor.class);
    register(MultiPartFeature.class);
    // validation errors will be sent to the client
    property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
  }
}
