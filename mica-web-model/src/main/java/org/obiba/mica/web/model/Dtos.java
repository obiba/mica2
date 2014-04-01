package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Network;
import org.obiba.mica.domain.Study;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.NetworkDto;
import static org.obiba.mica.web.model.Mica.NetworkDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;

@Component
public class Dtos {

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    return StudyDtos.asDto(study);
  }

  @NotNull
  public Study fromDto(@NotNull StudyDtoOrBuilder dto) {
    return StudyDtos.fromDto(dto);
  }

  @NotNull
  public NetworkDto asDto(@NotNull Network network) {
    return NetworkDtos.asDto(network);
  }

  @NotNull
  public Network fromDto(@NotNull NetworkDtoOrBuilder dto) {
    return NetworkDtos.fromDto(dto);
  }

}
