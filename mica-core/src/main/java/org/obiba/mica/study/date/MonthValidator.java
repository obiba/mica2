package org.obiba.mica.study.date;

public class MonthValidator implements Validator {
  @Override
  public void validate(int value) {
    if (value < 1 || value > 12 ) throw new IllegalArgumentException("Month must be between 1 and 12");
  }
}
