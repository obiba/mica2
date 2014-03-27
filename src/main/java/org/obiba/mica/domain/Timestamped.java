package org.obiba.mica.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

public interface Timestamped {

  @NotNull
  LocalDateTime getCreated();

  LocalDateTime getUpdated();

}
