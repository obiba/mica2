package org.obiba.mica.micaConfig;

import java.util.NoSuchElementException;

public class NoSuchDataAccessFormException extends NoSuchElementException {

  private static final long serialVersionUID = 5931123739037832740L;

  private NoSuchDataAccessFormException(String s) {
    super(s);
  }

  public static NoSuchDataAccessFormException withDefaultMessage() {
    return new NoSuchDataAccessFormException("DataAccessForm does not exist");
  }
}
