package org.obiba.mica.domain;

import java.util.Locale;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class NoSuchAttributeException extends NoSuchElementException {

  private static final long serialVersionUID = 5887330656285998606L;

  @NotNull
  private final String attributeName;

  public NoSuchAttributeException(@NotNull String attribute, @NotNull String attributeAware) {
    this(attribute, null, attributeAware);
  }

  public NoSuchAttributeException(@NotNull String attribute, @Nullable Locale locale, @NotNull String attributeAware) {
    super("No such attribute '" + attribute + (locale == null ? "'" : "'@" + locale.toString()) + " for '" +
        attributeAware + "'");
    attributeName = attribute;
  }

  @NotNull
  public String getAttributeName() {
    return attributeName;
  }

}
