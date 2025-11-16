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

import org.obiba.mica.core.domain.EntityState;
import org.springframework.stereotype.Component;


@Component
public class EntityStateDtos {

  @NotNull
  public Mica.EntityStateDto.Builder asDto(@NotNull EntityState state) {
    Mica.EntityStateDto.Builder builder = Mica.EntityStateDto.newBuilder()//
      .setRevisionsAhead(state.getRevisionsAhead()) //
      .setRevisionStatus(state.getRevisionStatus().name());

    if(state.isPublished()) {
      builder.setPublishedTag(state.getPublishedTag());
      if(state.hasPublishedId()) builder.setPublishedId(state.getPublishedId());
      if(state.hasPublicationDate()) builder.setPublicationDate(state.getPublicationDate().toString());
      if(state.getPublishedBy() != null) builder.setPublishedBy(state.getPublishedBy());
    }

    return builder;
  }
}
