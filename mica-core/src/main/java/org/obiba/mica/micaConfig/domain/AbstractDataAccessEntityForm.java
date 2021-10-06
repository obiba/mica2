package org.obiba.mica.micaConfig.domain;

import java.util.Map;

import org.joda.time.DateTime;
import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public abstract class AbstractDataAccessEntityForm extends EntityConfig {

  private int revision = 0;

  private DateTime lastUpdateDate;

  private Map<String, LocalizedString> properties;

  private String titleFieldPath;

  private String summaryFieldPath;

  private String endDateFieldPath;

  AbstractDataAccessEntityForm() {
    super();
  }

  public int getRevision() {
    return revision;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public DateTime getLastUpdateDate() {
    return lastUpdateDate == null ? DateTime.now() : lastUpdateDate;
  }

  public void setLastUpdateDate(DateTime lastUpdateDate) {
    this.lastUpdateDate = lastUpdateDate;
  }

  public Map<String, LocalizedString> getProperties() {
    return properties == null ? properties = Maps.newHashMap() : properties;
  }

  public void setProperties(Map<String, LocalizedString> properties) {
    this.properties = properties;
  }

  public String getTitleFieldPath() {
    return titleFieldPath;
  }

  public void setTitleFieldPath(String titleFieldPath) {
    this.titleFieldPath = titleFieldPath;
  }

  public boolean hasTitleFieldPath() {
    return !Strings.isNullOrEmpty(titleFieldPath);
  }

  public String getSummaryFieldPath() {
    return summaryFieldPath;
  }

  public void setSummaryFieldPath(String summaryFieldPath) {
    this.summaryFieldPath = summaryFieldPath;
  }

  public boolean hasSummaryFieldPath() {
    return !Strings.isNullOrEmpty(summaryFieldPath);
  }

  public void setEndDateFieldPath(String endDateFieldPath) {
    this.endDateFieldPath = endDateFieldPath;
  }

  public String getEndDateFieldPath() {
    return endDateFieldPath;
  }

  public boolean hasEndDateFieldPath() {
    return !Strings.isNullOrEmpty(endDateFieldPath);
  }

}
