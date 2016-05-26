package org.obiba.mica.micaConfig;

import java.util.NoSuchElementException;

public class NoSuchProjectFormException extends NoSuchElementException {

  private static final long serialVersionUID = 6298260614643553931L;

  private NoSuchProjectFormException(String s) {
    super(s);
  }

  public static NoSuchProjectFormException withDefaultMessage() {
    return new NoSuchProjectFormException("ProjectForm does not exist");
  }
}
