package org.obiba.mica.config;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.obiba.mica.micaConfig.rest.ConfigurationInterceptor;
import org.obiba.mica.web.rest.security.AuditInterceptor;
import org.obiba.mica.web.rest.security.AuthenticationInterceptor;
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
    register(ConfigurationInterceptor.class);
    register(AuditInterceptor.class);
    register(MultiPartFeature.class);
    // validation errors will be sent to the client
    property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
  }
}
