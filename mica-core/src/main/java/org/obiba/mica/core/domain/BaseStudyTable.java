/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

public class BaseStudyTable extends OpalTable {

  protected String studyId;

  protected String populationId;

  protected int populationWeight;

  public String getStudyId() {
    return studyId;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public String getPopulationId() {
    return populationId;
  }

  public String getPopulationUId() {
    return BaseStudyTable.getPopulationUId(studyId, populationId);
  }

  public static String getPopulationUId(String studyId, String populationId) {
    return new StringBuilder(studyId).append(":").append(populationId).toString();
  }

  public String getDataCollectionEventUId() {
    return getDataCollectionEventUId(studyId, populationId, ".");
  }

  public static String getDataCollectionEventUId(String studyId, String populationId, String dataCollectionEventId) {
    return new StringBuilder(studyId).append(":").append(populationId).append(":").append(dataCollectionEventId)
        .toString();
  }

  public static String getDataCollectionEventUId(String studyId) {
    return new StringBuilder(studyId).append(":").append(".:.").toString();
  }

  public void setPopulationId(String populationId) {
    this.populationId = populationId;
  }

  public int getPopulationWeight() {
    return populationWeight;
  }

  public void setPopulationWeight(int populationWeight) {
    this.populationWeight = populationWeight;
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
