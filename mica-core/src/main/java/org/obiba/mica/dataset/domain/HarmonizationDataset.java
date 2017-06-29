/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.obiba.mica.core.domain.HarmonizationTable;
import org.obiba.mica.core.domain.NetworkTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;

/**
 * Dataset that relies on Study Opal servers summaries.
 */
public class HarmonizationDataset extends Dataset {

  private static final long serialVersionUID = -658603952811380458L;

  /**
   * Linked Harmonization Table
   */
  private HarmonizationTable harmonizationLink;

  /**
   * Tables that implement the harmonization.
   */
  private List<StudyTable> studyTables;

  private List<HarmonizationTable> harmonizationTables;

  private List<NetworkTable> networkTables;

  /**
   * Linked network.
   */
  private String networkId;

  public HarmonizationTable getHarmonizationLink() {
    return harmonizationLink;
  }

  public boolean hasHarmonizationTable() {
    return harmonizationLink != null;
  }

  @JsonIgnore
  public HarmonizationTable getSafeHarmonizationLink() {
    if (!hasHarmonizationTable()) throw new IllegalArgumentException("Harmonization Dataset is missing a harmonization table");
    return harmonizationLink;
  }

  public void setHarmonizationLink(HarmonizationTable harmonizationLink) {
    this.harmonizationLink = harmonizationLink;
  }

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

  public List<HarmonizationTable> getHarmonizationTables() {
    return harmonizationTables == null ? harmonizationTables = new ArrayList<>() : harmonizationTables;
  }

  public void addHarmonizationTable(HarmonizationTable harmonizationTable) {
    getHarmonizationTables().add(harmonizationTable);
  }

  public void setHarmonizationTables(List<HarmonizationTable> harmonizationTables) {
    this.harmonizationTables = harmonizationTables;
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

  @JsonIgnore
  public List<OpalTable> getAllOpalTables() {
    return Lists.newArrayList(Iterables.concat(getStudyTables(), getHarmonizationTables(), getNetworkTables())).stream()//
      .sorted((a, b) -> a.getWeight() - b.getWeight()).collect(Collectors.toList());
  }
}
