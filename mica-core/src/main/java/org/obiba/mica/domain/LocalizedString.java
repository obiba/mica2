package org.obiba.mica.domain;

import java.util.Locale;
import java.util.TreeMap;

import javax.validation.constraints.NotNull;

public class LocalizedString extends TreeMap<Locale, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  public LocalizedString() {
    super((l1, l2) -> l1.getLanguage().compareTo(l2.getLanguage()));
  }

  public LocalizedString(@NotNull Locale locale, @NotNull String str) {
    this();
    put(locale, str);
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
