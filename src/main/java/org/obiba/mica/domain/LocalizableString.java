package org.obiba.mica.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotNull;

public class LocalizableString extends HashMap<Locale, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  public LocalizableString() {
  }

  public LocalizableString(@NotNull Locale locale, @NotNull String str) {
    put(locale, str);
  }

  public LocalizableString(Map<? extends Locale, ? extends String> m) {
    super(m);
  }

  public LocalizableString forLocale(@NotNull Locale locale, @NotNull String str) {
    put(locale, str);
    return this;
  }

  public LocalizableString forEn(@NotNull String str) {
    return forLocale(Locale.ENGLISH, str);
  }

  public LocalizableString forFr(@NotNull String str) {
    return forLocale(Locale.FRENCH, str);
  }

  public static LocalizableString en(@NotNull String str) {
    return new LocalizableString(Locale.ENGLISH, str);
  }

  public static LocalizableString fr(@NotNull String str) {
    return new LocalizableString(Locale.FRENCH, str);
  }
}
