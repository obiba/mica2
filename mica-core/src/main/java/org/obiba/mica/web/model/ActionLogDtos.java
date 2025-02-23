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

import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.ActionLogDto;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.springframework.stereotype.Component;

@Component
public class ActionLogDtos {

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private UserProfileDtos userProfileDtos;

  ActionLogDto asDto(ActionLog actionLog) {
    ActionLogDto.Builder builder = ActionLogDto.newBuilder() //
      .setAuthor(actionLog.getAuthor()) //
      .setChangedOn(actionLog.getChangedOn().toString())
      .setAction(actionLog.getAction());

    Subject profile = userProfileService.getProfile(actionLog.getAuthor());
    if (profile != null) {
      builder.setProfile(userProfileDtos.asDto(profile));
    }

    return builder.build();
  }

  ActionLog fromDto(ActionLogDto dto) {
    return ActionLog.newBuilder() //
      .author(dto.getAuthor()) //
      .changedOn(LocalDateTime.parse(dto.getChangedOn())) //
      .action(dto.getAction()) //
      .build(); //
  }
}
