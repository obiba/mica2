package org.obiba.mica.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotNull;

public class LocalizedString extends HashMap<Locale, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  public LocalizedString() {
  }

  public LocalizedString(@NotNull Locale locale, @NotNull String str) {
    put(locale, str);
  }

  public LocalizedString(Map<? extends Locale, ? extends String> m) {
    super(m);
  }

  public LocalizedString forLocale(@NotNull Locale locale, @NotNull String str) {
    put(locale, str);
    return this;
  }

  public LocalizedString forEn(@NotNull String str) {
    return forLocale(Locale.ENGLISH, str);
  }

  public LocalizedString forFr(@NotNull String str) {
    return forLocale(Locale.FRENCH, str);
  }

  public static LocalizedString en(@NotNull String str) {
    return new LocalizedString(Locale.ENGLISH, str);
  }

  public static LocalizedString fr(@NotNull String str) {
    return new LocalizedString(Locale.FRENCH, str);
  }
}
