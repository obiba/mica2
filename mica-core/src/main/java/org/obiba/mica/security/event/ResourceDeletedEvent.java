package org.obiba.mica.security.event;

import javax.validation.constraints.NotNull;

public class ResourceDeletedEvent {

  private String resource;

  private String instance;

  public ResourceDeletedEvent(@NotNull String resource, @NotNull String instance) {
    this.resource = resource;
    this.instance = instance;
  }

  @NotNull
  public String getResource() {
    return resource;
  }

  @NotNull
  public String getInstance() {
    return instance;
  }
}
