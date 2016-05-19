package org.obiba.mica.project.service;

import java.util.NoSuchElementException;

import org.obiba.mica.network.NoSuchNetworkException;

public class NoSuchMicaProjectException extends NoSuchElementException {

  private static final long serialVersionUID = 83912682720233942L;

  private NoSuchMicaProjectException(String s) {
    super(s);
  }

  public static NoSuchMicaProjectException withId(String id) {
    return new NoSuchMicaProjectException("Project with id '" + id + "' does not exist");
  }

}
