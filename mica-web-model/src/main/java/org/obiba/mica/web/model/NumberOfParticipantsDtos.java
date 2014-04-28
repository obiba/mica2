package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.NumberOfParticipants;

import static org.obiba.mica.web.model.Mica.StudyDto.NumberOfParticipantsDto;

class NumberOfParticipantsDtos {

  private NumberOfParticipantsDtos() {}

  @NotNull
  static NumberOfParticipantsDto asDto(@NotNull NumberOfParticipants numberOfParticipants) {
    NumberOfParticipantsDto.Builder builder = NumberOfParticipantsDto.newBuilder();
    if(numberOfParticipants.getParticipant() != null)
      builder.setParticipant(TargetNumberDtos.asDto(numberOfParticipants.getParticipant()));
    if(numberOfParticipants.getSample() != null)
      builder.setSample(TargetNumberDtos.asDto(numberOfParticipants.getSample()));
    if(numberOfParticipants.getInfo() != null)
      builder.addAllInfo(LocalizedStringDtos.asDto(numberOfParticipants.getInfo()));
    return builder.build();
  }


    @NotNull
  static NumberOfParticipants fromDto(@NotNull NumberOfParticipantsDto dto) {
      NumberOfParticipants numberOfParticipants = new NumberOfParticipants();
      if (dto.hasParticipant()) numberOfParticipants.setParticipant(TargetNumberDtos.fromDto(dto.getParticipant()));
      if (dto.hasSample()) numberOfParticipants.setSample(TargetNumberDtos.fromDto(dto.getSample()));
      if (dto.getInfoCount() > 0) numberOfParticipants.setInfo(LocalizedStringDtos.fromDto(dto.getInfoList()));
    return numberOfParticipants;
  }
}
