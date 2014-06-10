package org.obiba.mica.config;

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

@Configuration
public class JerseyConfiguration extends ResourceConfig {

  public static final String WS_ROOT = "/ws";

  @Bean
  public ServletRegistrationBean jerseyServlet() {
    ServletRegistrationBean registration = new ServletRegistrationBean(new ServletContainer(), WS_ROOT + "/*");
    registration.addInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS, JerseyServletConfig.class.getName());
    return registration;
  }

  public static class JerseyServletConfig extends ResourceConfig {
    public JerseyServletConfig() {
      register(RequestContextFilter.class);
      packages("org.obiba.mica", "org.obiba.jersey", "com.fasterxml.jackson");
      register(LoggingFilter.class);
      register(AuthenticationInterceptor.class);
      register(MultiPartFeature.class);
      // validation errors will be sent to the client
      property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }
  }

}
