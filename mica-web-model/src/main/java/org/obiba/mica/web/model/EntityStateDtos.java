package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

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
