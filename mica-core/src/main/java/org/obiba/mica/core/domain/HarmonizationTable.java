package org.obiba.mica.core.domain;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class HarmonizationTable extends OpalTable implements Serializable {

  private static final long serialVersionUID = -7165136303147086535L;

  private String harmonizationStudyId;

  private String populationId;

  public String getHarmonizationStudyId() {
    return harmonizationStudyId;
  }

  public void setHarmonizationStudyId(String harmonizationStudyId) {
    this.harmonizationStudyId = harmonizationStudyId;
  }

  public String getPopulationId() {
    return populationId;
  }

  public void setPopulationId(String populationId) {
    this.populationId = populationId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("project", getProject()).add("table", getTable())
      .add("harmonizationStudyId", getHarmonizationStudyId()).add("populationId", getPopulationId())
      .toString();
  }

  @Override
  protected String getEntityId() {
    return harmonizationStudyId;
  }
}
