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

import org.obiba.mica.core.domain.Timestamped;

class TimestampsDtos {

  private TimestampsDtos() {}

  static Mica.TimestampsDto asDto(Timestamped timestamped) {
    Mica.TimestampsDto.Builder builder = Mica.TimestampsDto.newBuilder()
        .setCreated(timestamped.getCreatedDate().get().toString());
    if(timestamped.getLastModifiedDate().isPresent()) builder.setLastUpdate(timestamped.getLastModifiedDate().get().toString());
    return builder.build();
  }

  static void fromDto(Mica.TimestampsDtoOrBuilder dto, Timestamped timestamped) {
    if(dto.hasCreated()) timestamped.setCreatedDate(LocalDateTime.parse(dto.getCreated()));
    if(dto.hasLastUpdate()) timestamped.setLastModifiedDate(LocalDateTime.parse(dto.getLastUpdate()));
  }
}
