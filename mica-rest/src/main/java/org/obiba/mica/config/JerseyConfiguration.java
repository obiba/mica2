/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.base.JsonMappingExceptionMapper;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.base.JsonParseExceptionMapper;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.obiba.mica.micaConfig.rest.ConfigurationInterceptor;
import org.obiba.mica.web.rest.security.AuditInterceptor;
import org.obiba.mica.web.rest.security.AuthenticationInterceptor;
import org.obiba.mica.web.rest.security.CSRFInterceptor;
import org.obiba.mica.web.rest.security.CSRFTokenHelper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath(JerseyConfiguration.WS_ROOT)
public class JerseyConfiguration extends ResourceConfig {

  public static final String WS_ROOT = "/ws";

  @Inject
  public JerseyConfiguration(Environment environment, CSRFTokenHelper csrfTokenHelper) {
    register(RequestContextFilter.class);
    packages("org.obiba.mica", "org.obiba.jersey");
    // Opt out of JacksonFeature's auto-registered DefaultJacksonJaxbJsonProvider and use MicaJacksonJsonProvider
    // instead: it does the same job but declines protobuf messages, which are the ProtobufJsonProvider's business.
    // Both providers declare MessageBodyWriter<Object> for application/json, so Jersey cannot order them by type or
    // media type distance and picks whichever comes first in an unordered set. When Jackson wins, writing a protobuf
    // DTO fails with "Direct self-reference leading to cycle" (UnknownFieldSet) and the response turns into an error.
    property(InternalProperties.JSON_FEATURE, MicaJacksonJsonProvider.class.getSimpleName());
    register(MicaJacksonJsonProvider.class);
    // exception mappers JacksonFeature would have registered along with its provider
    register(JsonParseExceptionMapper.class);
    register(JsonMappingExceptionMapper.class);
    register(AuthenticationInterceptor.class);
    register(ConfigurationInterceptor.class);
    register(AuditInterceptor.class);
    register(new CSRFInterceptor(
      environment.matchesProfiles(Profiles.PROD),
      environment.getProperty("csrf.allowed", ""),
      environment.getProperty("csrf.allowed-agents", ""),
      csrfTokenHelper
      ));
    register(MultiPartFeature.class);
    register(DefaultLocaleFilter.class);
    // validation errors will be sent to the client
    property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
  }

  private String getServerPort(Environment environment) {
    return environment.getProperty("server.port", "8082");
  }
}
