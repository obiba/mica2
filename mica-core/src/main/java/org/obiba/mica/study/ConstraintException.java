package org.obiba.mica.study;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintException extends RuntimeException {
  private Map<String, List<String>> conflicts = new HashMap<>();

  public ConstraintException() {
    super();
  }

  public ConstraintException(String message) {
    super(message);
  }

  public ConstraintException(Throwable e) {
    super(e);
  }

  public ConstraintException(String message, Throwable e) {
    super(message, e);
  }

  public ConstraintException(Map<String, List<String>> conflicts) {
    super();
    this.conflicts = conflicts;
  }

  public Map<String, List<String>> getConflicts() {
    return conflicts;
  }
}
