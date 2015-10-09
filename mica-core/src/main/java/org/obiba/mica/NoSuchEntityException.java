package org.obiba.mica;

import java.util.NoSuchElementException;

import javax.validation.constraints.NotNull;

public class NoSuchEntityException extends NoSuchElementException {

  public NoSuchEntityException(String message){
    super(message);
  }

  public static NoSuchEntityException withId(@NotNull Class clazz, String id) {
    return new NoSuchEntityException(String.format("%s with id %s not found.", clazz.getSimpleName(), id));
  }

  public static NoSuchEntityException withPath(@NotNull Class clazz, String path) {
    return new NoSuchEntityException(String.format("%s with path %s not found.", clazz.getSimpleName(), path));
  }
}
