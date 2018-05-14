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

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.ActionLogDto;
import org.springframework.stereotype.Component;

@Component
public class ActionLogDtos {

  ActionLogDto asDto(ActionLog actionLog) {
    ActionLogDto.Builder builder = ActionLogDto.newBuilder() //
      .setAuthor(actionLog.getAuthor()) //
      .setChangedOn(actionLog.getChangedOn().toString())
      .setAction(actionLog.getAction());

    return builder.build();
  }

  ActionLog fromDto(ActionLogDto dto) {
    return ActionLog.newBuilder() //
      .author(dto.getAuthor()) //
      .changedOn(DateTime.parse(dto.getChangedOn())) //
      .action(dto.getAction()) //
      .build(); //
  }
}
