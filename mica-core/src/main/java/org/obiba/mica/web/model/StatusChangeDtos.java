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

import java.util.Map;
import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StatusChangeDtos {

  @Inject
  private UserProfileService subjectProfileService;

  @Inject
  private UserProfileDtos userProfileDtos;

  StatusChangeDto.Builder asDtoBuilder(StatusChange statusChange) {
    StatusChangeDto.Builder builder = asMinimalistDtoBuilder(statusChange);

    ObibaRealm.Subject profile = subjectProfileService.getProfile(statusChange.getAuthor());
    if (profile != null) {
      builder.setProfile(userProfileDtos.asDto(profile));
    }

    return builder;
  }

  StatusChangeDto asDto(StatusChange statusChange) {
    return asDtoBuilder(statusChange).build();
  }

  StatusChangeDto.Builder asMinimalistDtoBuilder(StatusChange statusChange) {
    return StatusChangeDto.newBuilder() //
      .setFrom(statusChange.getFrom().toString()) //
      .setTo(statusChange.getTo().toString()) //
      .setAuthor(statusChange.getAuthor()) //
      .setChangedOn(statusChange.getChangedOn().toString());
  }
}
