package org.obiba.mica.micaConfig.domain;

import java.util.Map;

import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public abstract class AbstractDataAccessEntityForm extends EntityConfig {

  final static int DEFAULT_ID_LENGTH = 6;

  private String csvExportFormat;

  private Map<String, LocalizedString> properties;

  private String titleFieldPath;

  private String summaryFieldPath;

  AbstractDataAccessEntityForm() {
    super();
  }

  public String getCsvExportFormat() {
    return csvExportFormat;
  }

  public void setCsvExportFormat(String csvExportFormat) {
    this.csvExportFormat = csvExportFormat;
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
}
