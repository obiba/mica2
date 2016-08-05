package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.obiba.mica.micaConfig.service.PopulationConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/population")
public class PopulationConfigResource extends EntityConfigResource<PopulationConfig> {

  @Inject
  PopulationConfigService populationConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(PopulationConfig populationConfig) {
    return dtos.asDto(populationConfig);
  }

  @Override
  protected PopulationConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected PopulationConfigService getConfigService() {
    return populationConfigService;
  }
}
