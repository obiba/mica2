package org.obiba.mica.core.domain;

public class MaximumDocumentSetCreationExceededException extends RuntimeException {

  public MaximumDocumentSetCreationExceededException(String message) {
    super(message);
  }

  public static MaximumDocumentSetCreationExceededException because(long maximum, String type) {
    return new MaximumDocumentSetCreationExceededException("Attempted to create a \"" + type + "\" set while the max number of sets is configured to be: " + maximum + ".");
  }

}
