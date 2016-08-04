package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.service.StudyConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/study")
public class StudyConfigResource extends EntityConfigResource<StudyConfig> {

  @Inject
  StudyConfigService studyConfigService;

  @Inject
  Dtos dtos;

  @Override
  protected Mica.EntityFormDto asDto(StudyConfig studyConfig) {
    return dtos.asDto(studyConfig);
  }

  @Override
  protected StudyConfig fromDto(Mica.EntityFormDto dto) {
    return dtos.fromDto(dto);
  }

  @Override
  protected StudyConfigService getConfigService() {
    return studyConfigService;
  }
}
