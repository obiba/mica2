/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.jazdw.rql.converter.Converter;
import net.jazdw.rql.converter.ConverterException;
import net.jazdw.rql.converter.ValueConverter;

/**
 * Converter that does not make fancy number conversions when 'number:' is not specified.
 */
class RQLConverter extends Converter {

  RQLConverter() {
    super(new AutoValueConverter(), CONVERTERS);
  }

  private static class AutoValueConverter implements ValueConverter {
    /**
     * The default automatic conversion map
     */
    public static final Map<String, Object> DEFAULT_CONVERSIONS = new HashMap<>();
    static {
      DEFAULT_CONVERSIONS.put("true", Boolean.TRUE);
      DEFAULT_CONVERSIONS.put("false", Boolean.FALSE);
      DEFAULT_CONVERSIONS.put("null", null);
      DEFAULT_CONVERSIONS.put("Infinity", Double.POSITIVE_INFINITY);
      DEFAULT_CONVERSIONS.put("-Infinity", Double.NEGATIVE_INFINITY);
    }

    // detects ISO 8601 dates with a minimum of year, month and day specified
    private static final Pattern DATE_PATTERN = Pattern.compile("^[0-9]{4}-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])(T(2[0-3]|[01][0-9])(:[0-5][0-9])?(:[0-5][0-9])?(\\.[0-9][0-9]?[0-9]?)?(Z|[+-](?:2[0-3]|[01][0-9])(?::?(?:[0-5][0-9]))?)?)?$");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]+[\\.]*[0-9]*$");

    private Map<String, Object> conversions;

    AutoValueConverter() {
      this(DEFAULT_CONVERSIONS);
    }

    AutoValueConverter(Map<String, Object> autoConversionMap) {
      conversions = new HashMap<>(autoConversionMap);
    }

    public Object convert(String input) throws ConverterException {
      try {
        if (conversions.containsKey(input)) {
          return conversions.get(input);
        }

        try {
          if (NUMBER_PATTERN.matcher(input).matches()) {
            return Converter.NUMBER.convert(input);
          }
        } catch (ConverterException e) {}

        try {
          if (DATE_PATTERN.matcher(input).matches()) {
            return Converter.DATE.convert(input);
          }
        } catch (ConverterException e) {}

        return input;
      } catch (Exception e) {
        throw new ConverterException(e);
      }
    }
  }

}
