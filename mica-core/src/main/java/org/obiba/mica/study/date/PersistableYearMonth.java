/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.date;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PersistableYearMonth implements Serializable, Comparable<PersistableYearMonth> {

  private static final long serialVersionUID = 2808318271243499159L;

  private static final YearValidator YEAR_VALIDATOR = new YearValidator();
  private static final MonthValidator MONTH_VALIDATOR = new MonthValidator();

  private String yearMonth;

  public interface YearMonthData {
    int getYear();
    int getMonth();
  }

  public PersistableYearMonth() {}

  public String getYearMonth() {
    return yearMonth;
  }

  @JsonIgnore
  public String getSortableYearMonth() {
    return yearMonth != null ? yearMonth.replace("-", "") : null;
  }

  public static PersistableYearMonth of(int y, int m) {
    PersistableYearMonth instance = new PersistableYearMonth();
    YEAR_VALIDATOR.validate(y);
    MONTH_VALIDATOR.validate(m);
    instance.yearMonth = format(y, m);
    return instance;
  }

  public static PersistableYearMonth of(int y) {
    PersistableYearMonth instance = new PersistableYearMonth();
    YEAR_VALIDATOR.validate(y);
    instance.yearMonth = format(y);
    return instance;
  }

  public static String format(int year, int month) {
    return String.format("%4d-%02d", year, month);
  }

  public static String format (int year) {
    return String.format("%4d", year);
  }

  @JsonIgnore
  public YearMonthData getYearMonthData() {
    return parse(yearMonth);
  }

  public static YearMonthData parse(String text) {
    Pattern p = Pattern.compile("(\\d{4})-(\\d{1,2})");
    Matcher m = p.matcher(text);
    if (!m.matches()) {
      p = Pattern.compile("(\\d{4})");
      m = p.matcher(text);

      if (!m.matches()) throw new IllegalArgumentException("Invalid YearMonth format, expected YYYY-mm or YYYY: " + text);
    }

    int year = Integer.valueOf(m.group(1));
    int month = 0;
    if (m.groupCount() == 2) {
      month = Integer.valueOf(m.group(2));
      MONTH_VALIDATOR.validate(month);
    }
    YEAR_VALIDATOR.validate(year);

    int finalMonth = month;
    return new YearMonthData() {
      @Override
      public int getYear() {
        return year;
      }

      @Override
      public int getMonth() {
        return finalMonth;
      }
    };
  }

  @Override
  public int compareTo(PersistableYearMonth o) {
    return yearMonth.compareTo(o.yearMonth);
  }
}
