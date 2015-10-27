package org.obiba.mica.core.domain;

import javax.validation.constraints.NotNull;

public class InvalidRevisionStatusException extends RuntimeException {

  public InvalidRevisionStatusException(String message) {
    super(message);
  }

  public static InvalidRevisionStatusException withStatus(@NotNull RevisionStatus status) {
    return new InvalidRevisionStatusException(String.format("%s", status));
  }
}
