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
public class NetworkConfigResource extends EntityConfigResource<NetworkConfig> implements PermissionAwareResource {
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
