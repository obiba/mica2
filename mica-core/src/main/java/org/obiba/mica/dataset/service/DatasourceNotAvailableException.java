package org.obiba.mica.dataset.service;

public class DatasourceNotAvailableException extends RuntimeException {

  public DatasourceNotAvailableException() {
    super();
  }

  public DatasourceNotAvailableException(String message) {
    super(message);
  }

  public DatasourceNotAvailableException(Throwable t) {
    super(t);
  }

  public DatasourceNotAvailableException(String message, Throwable t) {
    super(message,  t);
  }

}
