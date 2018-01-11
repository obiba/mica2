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
import org.obiba.mica.core.domain.Timestamped;

class TimestampsDtos {

  private TimestampsDtos() {}

  static Mica.TimestampsDto asDto(Timestamped timestamped) {
    Mica.TimestampsDto.Builder builder = Mica.TimestampsDto.newBuilder()
        .setCreated(timestamped.getCreatedDate().toString());
    if(timestamped.getLastModifiedDate() != null) builder.setLastUpdate(timestamped.getLastModifiedDate().toString());
    return builder.build();
  }

  static void fromDto(Mica.TimestampsDtoOrBuilder dto, Timestamped timestamped) {
    if(dto.hasCreated()) timestamped.setCreatedDate(DateTime.parse(dto.getCreated()));
    if(dto.hasLastUpdate()) timestamped.setLastModifiedDate(DateTime.parse(dto.getLastUpdate()));
  }
}
