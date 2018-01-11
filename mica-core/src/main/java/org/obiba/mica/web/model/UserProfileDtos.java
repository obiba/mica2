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

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.obiba.shiro.realm.ObibaRealm;
import org.springframework.stereotype.Component;

@Component
public class UserProfileDtos {

  @NotNull
  public Mica.UserProfileDto asDto(ObibaRealm.Subject subject) {
    Mica.UserProfileDto.Builder builder = Mica.UserProfileDto.newBuilder().setUsername(subject.getUsername());
    List<String> groups = subject.getGroups();
    if (groups != null) {
      builder.addAllGroups(groups);
    }

    List<Map<String, String>> attributes = subject.getAttributes();
    if (attributes != null) {
      attributes.forEach(attribute -> builder.addAttributes(
          Mica.UserProfileDto.AttributeDto.newBuilder().setKey(attribute.get("key")).setValue(attribute.get("value")))
      );
    }

    return builder.build();
  }
}
