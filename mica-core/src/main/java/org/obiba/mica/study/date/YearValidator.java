package org.obiba.mica.study.date;

public class YearValidator implements Validator {
  @Override
  public void validate(int value) {
    if (value < 1900) throw new IllegalArgumentException("Year must be greater than 1900");

    if (value > 9999) throw new IllegalArgumentException("Year must be less or equal than 9999");
  }
}
