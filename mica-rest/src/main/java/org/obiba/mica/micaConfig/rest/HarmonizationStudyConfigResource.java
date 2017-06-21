package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.micaConfig.service.HarmonizationStudyConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/" + HarmonizationStudyConfigResource.TARGET_NAME)
public class HarmonizationStudyConfigResource
  extends EntityConfigResource<HarmonizationStudyConfig, Mica.EntityFormDto>
  implements PermissionAwareResource {

  static final String TARGET_NAME = "harmonization-study";

  @Inject
  HarmonizationStudyConfigService harmonizationStudyConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(HarmonizationStudyConfig entityConfig) {
    return dtos.asDto(entityConfig);
  }

  @Override
  protected HarmonizationStudyConfig fromDto(Mica.EntityFormDto entityConfig) {
    return dtos.fromDto(entityConfig);
  }

  @Override
  protected EntityConfigService<HarmonizationStudyConfig> getConfigService() {
    return harmonizationStudyConfigService;
  }

  @Override
  public String getTarget() {
    return TARGET_NAME;
  }
}
