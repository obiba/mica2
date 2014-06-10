package org.obiba.mica.file;

import java.util.NoSuchElementException;

import javax.validation.constraints.NotNull;

public class NoSuchTempFileException extends NoSuchElementException {

  private static final long serialVersionUID = 5887330656285998606L;

  @NotNull
  private final String id;

  public NoSuchTempFileException(@NotNull String id) {
    super("No such temp file '" + id + "'");
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
