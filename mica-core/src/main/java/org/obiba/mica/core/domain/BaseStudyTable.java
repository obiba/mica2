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
import com.google.common.base.Strings;

public class BaseStudyTable {

  protected String studyId;

  protected String populationId;

  protected int populationWeight;

  private LocalizedString name;

  private LocalizedString description;

  private LocalizedString additionalInformation;

  private int weight;

  private String sourceURN;

  // legacy
  private String project;

  // legacy
  private String table;

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
    return new StringBuilder(studyId).append(":").append(Strings.isNullOrEmpty(populationId) ? "." : populationId).toString();
  }

  public String getDataCollectionEventUId() {
    return getDataCollectionEventUId(studyId, populationId, ".");
  }

  public static String getDataCollectionEventUId(String studyId, String populationId, String dataCollectionEventId) {
    return new StringBuilder(BaseStudyTable.getPopulationUId(studyId, populationId))
      .append(":")
      .append(Strings.isNullOrEmpty(dataCollectionEventId) ? "." : dataCollectionEventId)
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
    return MoreObjects.toStringHelper(this).add("source", getSourceURN())
      .add("studyId", getStudyId()).add("populationId", getPopulationId())
      .toString();
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

  public LocalizedString getAdditionalInformation() {
    return additionalInformation;
  }

  public void setAdditionalInformation(LocalizedString additionalInformation) {
    this.additionalInformation = additionalInformation;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public boolean isFor(String studyId, String sourceURN) {
    return this.studyId.equals(studyId) && getSourceURN().equals(sourceURN);
  }

  public void setSourceURN(String sourceURN) {
    this.sourceURN = sourceURN;
  }

  public String getSourceURN() {
    // legacy
    if (Strings.isNullOrEmpty(sourceURN)) {
      this.sourceURN = OpalTableSource.newSource(project, table).getURN();
    }
    return sourceURN;
  }

  @Deprecated
  public void setProject(String project) {
    this.project = project;
  }

  @Deprecated
  public void setTable(String table) {
    this.table = table;
  }
}
