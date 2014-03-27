package org.obiba.mica.domain;

import java.util.HashMap;
import java.util.Locale;

import javax.validation.constraints.NotNull;

public class LocalizableString extends HashMap<Locale, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  public LocalizableString(Locale locale, @NotNull String str) {
    put(locale, str);
  }

  public LocalizableString(@NotNull String str) {
    this(null, str);
  }

}
