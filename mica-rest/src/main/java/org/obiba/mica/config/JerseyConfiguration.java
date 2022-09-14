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

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.obiba.mica.micaConfig.rest.ConfigurationInterceptor;
import org.obiba.mica.web.rest.security.AuditInterceptor;
import org.obiba.mica.web.rest.security.AuthenticationInterceptor;
import org.obiba.mica.web.rest.security.CSRFInterceptor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;

@Component
@ApplicationPath(JerseyConfiguration.WS_ROOT)
public class JerseyConfiguration extends ResourceConfig {

  public static final String WS_ROOT = "/ws";

  @Inject
  public JerseyConfiguration(Environment environment) {
    register(RequestContextFilter.class);
    packages("org.obiba.mica", "org.obiba.jersey", "com.fasterxml.jackson");
    // register(LoggingFeature.class);
    register(AuthenticationInterceptor.class);
    register(ConfigurationInterceptor.class);
    register(AuditInterceptor.class);
    register(new CSRFInterceptor(environment.acceptsProfiles(Profiles.PROD), environment.getProperty("csrf.allowed", "")));
    register(MultiPartFeature.class);
    // validation errors will be sent to the client
    property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
  }

  private String getServerPort(Environment environment) {
    return environment.getProperty("server.port", "8082");
  }
}
