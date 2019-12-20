package org.obiba.mica.access.domain;

import com.google.common.collect.Lists;

import java.util.Date;
import java.util.List;

/**
 * Time line that represents the intermediate and final reports.
 */
public class DataAccessRequestTimeline {

  private Date startDate;

  private Date endDate;

  private List<Date> intermediateDates = Lists.newArrayList();

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public boolean hasEndDate() {
    return endDate != null;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getStartDate() {
    return startDate;
  }

  public boolean hasStartDate() {
    return startDate != null;
  }

  public void addIntermediateDate(Date date) {
    intermediateDates.add(date);
  }

  public List<Date> getIntermediateDates() {
    return intermediateDates;
  }

  public boolean hasIntermediateDates() {
    return !intermediateDates.isEmpty();
  }

}
