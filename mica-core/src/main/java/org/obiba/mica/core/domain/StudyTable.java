/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

/**
 * Represents
 */
public class StudyTable implements Serializable {

  private static final long serialVersionUID = -2466526849186256653L;

  private String studyId;

  private String populationId;

  private String dataCollectionEventId;

  @NotNull
  private String project;

  @NotNull
  private String table;

  public String getId() {
    return getStudyId() + ":" + getPopulationId() + ":" + getDataCollectionEventId();
  }

  public void setId(String ignored) {
    
  }

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

  public String getDataCollectionEventId() {
    return dataCollectionEventId;
  }

  public void setDataCollectionEventId(String dataCollectionEventId) {
    this.dataCollectionEventId = dataCollectionEventId;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }
}
