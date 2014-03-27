package org.obiba.mica.domain;

import java.util.Date;

import javax.validation.constraints.NotNull;

public interface Timestamped {

  @NotNull
  Date getCreated();

  Date getUpdated();

}
