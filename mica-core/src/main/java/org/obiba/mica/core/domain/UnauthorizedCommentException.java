package org.obiba.mica.core.domain;

public class UnauthorizedCommentException extends RuntimeException {

  private static final long serialVersionUID = -1501889273971119074L;

  public UnauthorizedCommentException(String message) {
    super(message);
  }
}
