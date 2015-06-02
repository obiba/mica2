/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.springframework.stereotype.Component;

@Component
public class StatusChangeDtos {

  StatusChangeDto asDto(StatusChange statusChange) {
    return StatusChangeDto.newBuilder() //
      .setFrom(statusChange.getFrom().toString()) //
      .setTo(statusChange.getTo().toString()) //
      .setAuthor(statusChange.getAuthor()) //
      .setChangedOn(statusChange.getChangedOn().toString())
      .build();
  }

  StatusChange fromDto(StatusChangeDto dto) {
    return StatusChange.newBuilder() //
      .previous(DataAccessRequest.Status.valueOf(dto.getFrom())) //
      .current(DataAccessRequest.Status.valueOf(dto.getTo())) //
      .author(dto.getAuthor()) //
      .changedOn(DateTime.parse(dto.getChangedOn())) //
      .build(); //
  }


}
