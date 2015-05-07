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

  public static PersistableYearMonth of(int y, int m) {
    PersistableYearMonth instance = new PersistableYearMonth();
    YEAR_VALIDATOR.validate(y);
    MONTH_VALIDATOR.validate(m);
    instance.yearMonth = format(y, m);
    return instance;
  }

  public static String format(int year, int month) {
    return String.format("%4d-%02d", year, month);
  }

  @JsonIgnore
  public YearMonthData getYearMonthData() {
    return parse(yearMonth);
  }

  public static YearMonthData parse(String text) {
    Pattern p = Pattern.compile("(\\d{4})-(\\d{1,2})");
    Matcher m = p.matcher(text);
    if (!m.matches()) throw new IllegalArgumentException("Invalid YearMonth format, expectes YYYY-mm: " + text);

    int year = Integer.valueOf(m.group(1));
    int month = Integer.valueOf(m.group(2));
    YEAR_VALIDATOR.validate(year);
    MONTH_VALIDATOR.validate(month);
    return new YearMonthData() {
      @Override
      public int getYear() {
        return year;
      }

      @Override
      public int getMonth() {
        return month;
      }
    };
  }

  @Override
  public int compareTo(PersistableYearMonth o) {
    return yearMonth.compareTo(o.yearMonth);
  }
}
