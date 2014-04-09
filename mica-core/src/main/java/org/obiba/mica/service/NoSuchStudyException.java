package org.obiba.mica.service;

import java.util.NoSuchElementException;

public class NoSuchStudyException extends NoSuchElementException {

  private static final long serialVersionUID = 5931123739037832740L;

  private NoSuchStudyException(String s) {
    super(s);
  }

  public static NoSuchStudyException withId(String id) {
    return new NoSuchStudyException("Study with id '" + id + "' does not exist");
  }

}
