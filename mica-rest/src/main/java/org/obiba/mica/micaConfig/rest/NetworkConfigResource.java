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

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.NetworkConfig;
import org.obiba.mica.micaConfig.service.NetworkConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/" + NetworkConfigResource.TARGET_NAME)
public class NetworkConfigResource
  extends EntityConfigResource<NetworkConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "network";

  @Inject
  NetworkConfigService networkConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(NetworkConfig networkConfig) {
    return dtos.asDto(networkConfig);
  }

  @Override
  protected NetworkConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected NetworkConfigService getConfigService() {
    return networkConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
