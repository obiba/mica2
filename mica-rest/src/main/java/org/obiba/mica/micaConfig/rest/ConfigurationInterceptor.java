/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.io.IOException;

@Priority(Integer.MIN_VALUE)
@Component
public class ConfigurationInterceptor implements ContainerResponseFilter {

  @Inject
  private MicaConfigService configService;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    throws IOException {
    MicaConfig config = configService.getConfig();
    if(config != null && config.getMicaVersion() != null)
      responseContext.getHeaders().putSingle("X-Mica-Version", config.getMicaVersion().toString());
  }
}
