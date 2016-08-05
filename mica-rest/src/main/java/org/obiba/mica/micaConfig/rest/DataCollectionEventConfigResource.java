package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.service.DataCollectionEventConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/data-collection-event")
public class DataCollectionEventConfigResource extends EntityConfigResource<DataCollectionEventConfig> {

  @Inject
  DataCollectionEventConfigService dataCollectionEventConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(DataCollectionEventConfig dataCollectionEventConfig) {
    return dtos.asDto(dataCollectionEventConfig);
  }

  @Override
  protected DataCollectionEventConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected DataCollectionEventConfigService getConfigService() {
    return dataCollectionEventConfigService;
  }
}
