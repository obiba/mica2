package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.domain.Network;
import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.StudyState;
import org.obiba.mica.service.study.StudyService;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.MicaConfigDto;
import static org.obiba.mica.web.model.Mica.MicaConfigDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.NetworkDto;
import static org.obiba.mica.web.model.Mica.NetworkDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudySummaryDto;

@Component
@SuppressWarnings("OverlyCoupledClass")
public class Dtos {

  @Inject
  private StudyService studyService;

  @Inject
  private StudyDtos studyDtos;

  @Inject
  private MicaConfigDtos micaConfigDtos;

  @Inject
  private NetworkDtos networkDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    return studyDtos.asDto(study, studyService.findStateByStudy(study));
  }

  @NotNull
  public StudySummaryDto asDto(@NotNull StudyState studyState) {
    return studySummaryDtos.asDto(studyState);
  }

  @NotNull
  public Study fromDto(@NotNull StudyDtoOrBuilder dto) {
    return studyDtos.fromDto(dto);
  }

  @NotNull
  public NetworkDto asDto(@NotNull Network network) {
    return networkDtos.asDto(network);
  }

  @NotNull
  public Network fromDto(@NotNull NetworkDtoOrBuilder dto) {
    return networkDtos.fromDto(dto);
  }

  @NotNull
  public MicaConfigDto asDto(@NotNull MicaConfig micaConfig) {
    return micaConfigDtos.asDto(micaConfig);
  }

  @NotNull
  public MicaConfig fromDto(@NotNull MicaConfigDtoOrBuilder dto) {
    return micaConfigDtos.fromDto(dto);
  }

}
