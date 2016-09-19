/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.NetworkTable;
import org.obiba.mica.core.domain.StudyTable;

/**
 * Dataset that relies on Study Opal servers summaries.
 */
public class HarmonizationDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  /**
   * Tables that implement the harmonization.
   */
  private List<StudyTable> studyTables;

  private List<NetworkTable> networkTables;

  /**
   * Linked network.
   */
  private String networkId;

  /**
   * Project in which the table is located.
   */
  @NotNull
  private String project;

  /**
   * Table that holds the variables in the primary Opal.
   */
  @NotNull
  private String table;

  @NotNull
  public List<StudyTable> getStudyTables() {
    return studyTables == null ? studyTables = new ArrayList<>() : studyTables;
  }

  public void addStudyTable(StudyTable studyTable) {
    getStudyTables().add(studyTable);
  }

  public void setStudyTables(List<StudyTable> studyTables) {
    this.studyTables = studyTables;
  }

  public List<NetworkTable> getNetworkTables() {
    return networkTables == null ? networkTables = new ArrayList<>() : networkTables;
  }

  public void setNetworkTables(List<NetworkTable> networkTables) {
    this.networkTables = networkTables;
  }

  public void addNetworkTable(NetworkTable networkTable) {
    getNetworkTables().add(networkTable);
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

  @Override
  public String pathPrefix() {
    return "harmonizationDatasets";
  }

  @Override
  public Map<String, Serializable> parts() {
    HarmonizationDataset self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  public String getNetworkId() {
    return networkId;
  }

  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }
}
