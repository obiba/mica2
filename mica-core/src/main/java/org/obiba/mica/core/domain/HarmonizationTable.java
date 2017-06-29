package org.obiba.mica.core.domain;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class HarmonizationTable extends OpalTable implements Serializable {

  private static final long serialVersionUID = -7165136303147086535L;

  private String studyId;

  private String populationId;

  public String getStudyId() {
    return studyId;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
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
      .add("studyId", getStudyId()).add("populationId", getPopulationId())
      .toString();
  }

  @Override
  protected String getEntityId() {
    return studyId;
  }
}
