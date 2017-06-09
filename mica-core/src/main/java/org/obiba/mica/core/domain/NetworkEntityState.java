package org.obiba.mica.core.domain;

public abstract class NetworkEntityState extends EntityState {

  @Override
  public String pathPrefix() {
    return "networks";
  }
}
