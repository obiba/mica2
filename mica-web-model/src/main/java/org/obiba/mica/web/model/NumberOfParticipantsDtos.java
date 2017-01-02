/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.study.domain.NumberOfParticipants;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.StudyDto.NumberOfParticipantsDto;

@Component
class NumberOfParticipantsDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  NumberOfParticipantsDto asDto(@NotNull NumberOfParticipants numberOfParticipants) {
    NumberOfParticipantsDto.Builder builder = NumberOfParticipantsDto.newBuilder();
    if(numberOfParticipants.getParticipant() != null) {
      builder.setParticipant(TargetNumberDtos.asDto(numberOfParticipants.getParticipant()));
    }
    if(numberOfParticipants.getSample() != null) {
      builder.setSample(TargetNumberDtos.asDto(numberOfParticipants.getSample()));
    }
    if(numberOfParticipants.getInfo() != null) {
      builder.addAllInfo(localizedStringDtos.asDto(numberOfParticipants.getInfo()));
    }
    return builder.build();
  }

  @NotNull
  NumberOfParticipants fromDto(@NotNull Mica.StudyDto.NumberOfParticipantsDtoOrBuilder dto) {
    NumberOfParticipants numberOfParticipants = new NumberOfParticipants();
    if(dto.hasParticipant()) numberOfParticipants.setParticipant(TargetNumberDtos.fromDto(dto.getParticipant()));
    if(dto.hasSample()) numberOfParticipants.setSample(TargetNumberDtos.fromDto(dto.getSample()));
    if(dto.getInfoCount() > 0) numberOfParticipants.setInfo(localizedStringDtos.fromDto(dto.getInfoList()));
    return numberOfParticipants;
  }
}
