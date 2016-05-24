package org.obiba.mica.project.service;

import java.util.NoSuchElementException;

public class NoSuchProjectException extends NoSuchElementException {

  private static final long serialVersionUID = 83912682720233942L;

  private NoSuchProjectException(String s) {
    super(s);
  }

  public static NoSuchProjectException withId(String id) {
    return new NoSuchProjectException("Project with id '" + id + "' does not exist");
  }

}
