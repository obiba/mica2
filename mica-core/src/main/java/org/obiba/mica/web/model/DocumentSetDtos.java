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


import org.obiba.mica.core.domain.DocumentSet;
import org.springframework.stereotype.Component;

@Component
class DocumentSetDtos {

  Mica.DocumentSetDto asDto(DocumentSet documentSet) {
    Mica.DocumentSetDto.Builder builder = Mica.DocumentSetDto.newBuilder()
      .setId(documentSet.getId())
      .setType(documentSet.getType())
      .setTimestamps(TimestampsDtos.asDto(documentSet))
      .setCount(documentSet.getIdentifiers().size());

    if (documentSet.hasName()) builder.setName(documentSet.getName());
    if (documentSet.hasUsername()) builder.setUsername(documentSet.getUsername());

    return builder.build();
  }

}
