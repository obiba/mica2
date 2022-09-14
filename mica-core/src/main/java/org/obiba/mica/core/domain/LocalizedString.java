/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.core.util.StringUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static java.text.Normalizer.normalize;

public class LocalizedString extends TreeMap<String, String> {

  private static final long serialVersionUID = 5813178087884887246L;

  private static final String LANGUAGE_TAG_UNDETERMINED = "und";

  public LocalizedString() {}

  public LocalizedString(@NotNull Locale locale, @NotNull String str) {
    this();
    put(locale, str);
  }

  @Override
  public String put(@Nullable String locale, String value) {
    if(Strings.isNullOrEmpty(value)) return null;
    return super.put(locale == null ? LANGUAGE_TAG_UNDETERMINED : Locale.forLanguageTag(locale).toLanguageTag(), value);
  }

  public String put(@NotNull Locale locale, @NotNull String str) {
    if(Strings.isNullOrEmpty(str)) return null;
    return super.put(locale == null ? LANGUAGE_TAG_UNDETERMINED : locale.toLanguageTag(), str);
  }

  /**
   * Get the value for the undetermined language.
   *
   * @return null if not found
   */
  @JsonIgnore
  @Nullable
  public String getUndetermined() {
    return get(LANGUAGE_TAG_UNDETERMINED);
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
    if(values == null) return this;
    values.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
    return this;
  }

  public String asUrlSafeString() {
    List<String> values = Lists.newArrayList();
    values().forEach(value -> {
      String normalized = decompose(value.replace(' ', '-')) //
        .replaceAll("[^A-Za-z0-9_\\.\\-~]", "_"); //NOTICE: permitted chars RFC 3986
      if(!values.contains(normalized)) values.add(normalized);
    });
    return StringUtil.collectionToString(values, "-");
  }

  public LocalizedString asAcronym() {
    LocalizedString acronym = new LocalizedString();
    entrySet().forEach(entry -> {
      String name = entry.getValue();
      StringBuffer buffer = new StringBuffer();
      for(char c : decompose(name).toCharArray()) {
        if(Character.isUpperCase(c)) buffer.append(c);
      }

      // default strategy
      String value = name.toUpperCase();

      if(buffer.length() > 0) {
        value = buffer.toString();
      } else {
        // extract first character of each word
        String[] letters = name.split("(?<=[\\S])[\\S]*\\s*");
        if(letters.length > 1) {
          value = Stream.of(letters).map(s -> s.toUpperCase()).collect(Collectors.joining());
        }
      }

      acronym.put(entry.getKey(), value);
    });
    return acronym;
  }

  private String decompose(String s) {
    return normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
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

  public static LocalizedString from(Map<String, String> map) {
    LocalizedString string = new LocalizedString();
    if(map != null) string.putAll(map);
    return string;
  }

  /**
   * Same string for each locale.
   *
   * @param locales
   * @param str
   * @return
   */
  public static LocalizedString from(@NotNull List<Locale> locales, @NotNull String str) {
    LocalizedString string = new LocalizedString();
    locales.forEach(locale -> string.put(locale, str));
    return string;
  }
}
