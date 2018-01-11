/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search.support;

import java.util.Locale;

final public class AggregationHelper {

  public static final String FIELD_SEPARATOR = ".";

  public static final String PROPERTIES = FIELD_SEPARATOR + "properties";
  public static final String LOCALIZED = PROPERTIES + FIELD_SEPARATOR + "localized";
  public static final String ALIAS = PROPERTIES + FIELD_SEPARATOR + "alias";
  public static final String RANGES = PROPERTIES + FIELD_SEPARATOR + "ranges";
  public static final String TYPE = PROPERTIES + FIELD_SEPARATOR + "type";

  public static final String NAME_SEPARATOR = "-";
  public static final String AGG_TERMS = "terms";
  public static final String AGG_STATS = "stats";
  public static final String AGG_RANGE = "range";
  public static final String UND_LOCALE = Locale.forLanguageTag("und").toLanguageTag();
  public static final String UND_LOCALE_NAME = NAME_SEPARATOR + UND_LOCALE;
  public static final String UND_LOCALE_FIELD = FIELD_SEPARATOR + UND_LOCALE;
  public static final String DEFAULT_LOCALE = Locale.ENGLISH.getLanguage();
  public static final String DEFAULT_LOCALE_NAME = NAME_SEPARATOR + DEFAULT_LOCALE;
  public static final String DEFAULT_LOCALE_FIELD = FIELD_SEPARATOR + DEFAULT_LOCALE;

  public static String formatName(String name) {
    return name.replaceAll("\\" + FIELD_SEPARATOR, NAME_SEPARATOR);
  }

  public static String unformatName(String name) {
    return name.replaceAll(NAME_SEPARATOR, FIELD_SEPARATOR);
  }

}
