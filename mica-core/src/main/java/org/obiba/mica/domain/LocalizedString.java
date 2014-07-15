package org.obiba.mica.domain;

import java.util.Locale;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import sun.util.locale.LanguageTag;

public class LocalizedString extends TreeMap<String, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  public LocalizedString() {}

  public LocalizedString(@NotNull Locale locale, @NotNull String str) {
    this();
    put(locale, str);
  }

  @Override
  public String put(@Nullable String locale, String value) {
    return super.put(locale == null ? LanguageTag.UNDETERMINED : Locale.forLanguageTag(locale).toLanguageTag(), value);
  }

  public String put(@NotNull Locale locale, @NotNull String str) {
    return super.put(locale == null ? LanguageTag.UNDETERMINED : locale.toLanguageTag(),str);
  }

  public LocalizedString forLocale(@NotNull Locale locale, @NotNull String str) {
    put(locale.toLanguageTag(), str);
    return this;
  }

  public LocalizedString forLanguageTag(@Nullable String locale, @NotNull String str) {
    return forLocale(locale == null ? Locale.forLanguageTag("und") : Locale.forLanguageTag(locale), str);
  }

  public LocalizedString forEn(@NotNull String str) {
    return forLocale(Locale.ENGLISH, str);
  }

  public LocalizedString forFr(@NotNull String str) {
    return forLocale(Locale.FRENCH, str);
  }

  public boolean contains(@Nullable Locale locale) {
    return containsKey(locale);
  }

  public LocalizedString merge(LocalizedString values) {
    if (values == null) return this;
    values.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
    return this;
  }

  //
  // Static methods
  //

  public static LocalizedString en(@NotNull String str) {
    return new LocalizedString(Locale.ENGLISH, str);
  }

  public static LocalizedString fr(@NotNull String str) {
    return new LocalizedString(Locale.FRENCH, str);
  }

}
