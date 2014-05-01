package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import static org.obiba.mica.domain.NumberOfParticipants.TargetNumber;
import static org.obiba.mica.web.model.Mica.TargetNumberDto;

class TargetNumberDtos {

  private TargetNumberDtos() {}

  static TargetNumberDto asDto(@NotNull TargetNumber targetNumber) {
    TargetNumberDto.Builder builder = TargetNumberDto.newBuilder().setNoLimit(targetNumber.isNoLimit());
    if(targetNumber.getNumber() != null) builder.setNumber(targetNumber.getNumber());
    return builder.build();
  }

  static TargetNumber fromDto(@NotNull TargetNumberDto dto) {
    TargetNumber targetNumber = new TargetNumber();
    if(dto.hasNoLimit()) targetNumber.setNoLimit(dto.getNoLimit());
    if(dto.hasNumber()) targetNumber.setNumber(dto.getNumber());
    return targetNumber;
  }

}