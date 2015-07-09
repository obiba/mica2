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

  private LocalizedString name;

  private LocalizedString description;

  /**
   * {@link org.obiba.mica.study.domain.DataCollectionEvent} unique ID (including {@link org.obiba.mica.study.domain.Study} ID
   * and {@link org.obiba.mica.study.domain.Population} ID.
   *
   * @return
   */
  public String getDataCollectionEventUId() {
    return getDataCollectionEventUId(getStudyId(), getPopulationId(), getDataCollectionEventId());
  }

  public static String getDataCollectionEventUId(String studyId, String populationId, String dataCollectionEventId) {
    return new StringBuilder(studyId).append(":").append(populationId).append(":").append(dataCollectionEventId)
      .toString();
  }

  public void setDataCollectionEventUId(String ignored) {
    // for jackson serializer
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

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getName() {
    return name;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public boolean isFor(String studyId, String project, String table) {
    return this.studyId.equals(studyId) && this.project.equals(project) && this.table.equals(table);
  }

  public boolean appliesTo(String studyId, String populationId, String dataCollectionEventId) {
    return this.studyId.equals(studyId) && this.populationId.equals(populationId) && this.dataCollectionEventId.equals(dataCollectionEventId);
  }
}
