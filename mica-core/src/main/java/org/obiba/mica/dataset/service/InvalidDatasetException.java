package org.obiba.mica.dataset.service;

public class InvalidDatasetException extends RuntimeException {

  public InvalidDatasetException() {
    super();
  }

  public InvalidDatasetException(String message) {
    super(message);
  }

  public InvalidDatasetException(Throwable e) {
    super(e);
  }

  public InvalidDatasetException(String message, Throwable e) {
    super(message, e);
  }
}
