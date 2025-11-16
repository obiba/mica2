/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import jakarta.validation.constraints.NotNull;

import static org.obiba.mica.study.domain.NumberOfParticipants.TargetNumber;
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
